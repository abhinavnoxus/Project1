package com.nexora.app.domain

import com.nexora.app.domain.alarm.AlarmMath
import com.nexora.app.domain.model.Alarm
import com.nexora.app.domain.model.AlarmKind
import com.nexora.app.domain.model.TimeOfDay
import com.nexora.app.domain.model.WeekdayMask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmMathTest {

    private val utc: ZoneId = ZoneId.of("UTC")

    private fun at(zone: ZoneId, y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `disabled alarm never fires`() {
        val alarm = Alarm(hour = 7, minute = 0, enabled = false)
        assertNull(AlarmMath.nextTrigger(alarm, at(utc, 2026, 3, 10, 6, 0), utc))
    }

    @Test
    fun `daily alarm fires later the same day`() {
        val alarm = Alarm(hour = 7, minute = 30, repeat = WeekdayMask.EveryDay)
        val now = at(utc, 2026, 3, 10, 6, 0) // Tuesday
        val next = AlarmMath.nextTrigger(alarm, now, utc)
        assertNotNull(next)
        assertEquals(at(utc, 2026, 3, 10, 7, 30), next)
    }

    @Test
    fun `daily alarm that already passed fires tomorrow`() {
        val alarm = Alarm(hour = 7, minute = 30, repeat = WeekdayMask.EveryDay)
        val now = at(utc, 2026, 3, 10, 8, 0)
        assertEquals(at(utc, 2026, 3, 11, 7, 30), AlarmMath.nextTrigger(alarm, now, utc))
    }

    @Test
    fun `alarm exactly now is not re-fired`() {
        val alarm = Alarm(hour = 7, minute = 0, repeat = WeekdayMask.EveryDay)
        val now = at(utc, 2026, 3, 10, 7, 0)
        assertEquals(at(utc, 2026, 3, 11, 7, 0), AlarmMath.nextTrigger(alarm, now, utc))
    }

    @Test
    fun `weekday alarm skips the weekend`() {
        val alarm = Alarm(
            hour = 6, minute = 45,
            repeat = WeekdayMask.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        )
        // Friday 2026-03-13 at 09:00 -> next is Monday 2026-03-16 06:45
        val friday = at(utc, 2026, 3, 13, 9, 0)
        assertEquals(at(utc, 2026, 3, 16, 6, 45), AlarmMath.nextTrigger(alarm, friday, utc))
    }

    @Test
    fun `weekend only alarm`() {
        val alarm = Alarm(
            hour = 9, minute = 0,
            repeat = WeekdayMask.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        )
        val wednesday = at(utc, 2026, 3, 11, 12, 0)
        assertEquals(at(utc, 2026, 3, 14, 9, 0), AlarmMath.nextTrigger(alarm, wednesday, utc))
    }

    @Test
    fun `one shot alarm returns its instant once`() {
        val target = at(utc, 2026, 3, 10, 15, 0)
        val alarm = Alarm(kind = AlarmKind.OneShot, oneShotAt = target, enabled = true)
        assertEquals(target, AlarmMath.nextTrigger(alarm, target - 1000, utc))
        assertNull(AlarmMath.nextTrigger(alarm, target + 1, utc))
    }

    @Test
    fun `non repeating alarm without mask schedules the next occurrence of the time`() {
        val alarm = Alarm(hour = 23, minute = 15, repeat = WeekdayMask.None, kind = AlarmKind.Reminder)
        val now = at(utc, 2026, 3, 10, 23, 30)
        assertEquals(at(utc, 2026, 3, 11, 23, 15), AlarmMath.nextTrigger(alarm, now, utc))
    }

    @Test
    fun `time zone change shifts the wall clock but keeps local time`() {
        val alarm = Alarm(hour = 7, minute = 0, repeat = WeekdayMask.EveryDay)
        val nowUtc = at(utc, 2026, 3, 10, 5, 0)
        val kolkata = ZoneId.of("Asia/Kolkata")
        val nextUtc = AlarmMath.nextTrigger(alarm, nowUtc, utc)
        val nextKolkata = AlarmMath.nextTrigger(alarm, nowUtc, kolkata)
        assertNotNull(nextUtc)
        assertNotNull(nextKolkata)
        // Same instant is not required, but each must resolve to 07:00 local.
        val local = java.time.Instant.ofEpochMilli(nextKolkata!!).atZone(kolkata).toLocalTime()
        assertEquals(7, local.hour)
        assertEquals(0, local.minute)
    }

    @Test
    fun `dst spring forward does not lose the alarm`() {
        // US Eastern: 2026-03-08 02:00 -> 03:00. An alarm at 02:30 lands in the gap.
        val eastern = ZoneId.of("America/New_York")
        val alarm = Alarm(hour = 2, minute = 30, repeat = WeekdayMask.EveryDay)
        val now = at(eastern, 2026, 3, 8, 0, 30)
        val next = AlarmMath.nextTrigger(alarm, now, eastern)
        assertNotNull(next)
        val local = java.time.Instant.ofEpochMilli(next!!).atZone(eastern)
        assertTrue("expected a fire on the same day, got $local", local.dayOfMonth == 8)
    }

    @Test
    fun `dst fall back does not double fire`() {
        val eastern = ZoneId.of("America/New_York")
        val alarm = Alarm(hour = 1, minute = 30, repeat = WeekdayMask.EveryDay)
        val now = at(eastern, 2026, 10, 31, 12, 0)
        val next = AlarmMath.nextTrigger(alarm, now, eastern)
        assertNotNull(next)
        val local = java.time.Instant.ofEpochMilli(next!!).atZone(eastern).toLocalTime()
        assertEquals(1, local.hour)
        assertEquals(30, local.minute)
    }

    @Test
    fun `alarm survives a zone change while scheduled`() {
        val alarm = Alarm(hour = 6, minute = 0, repeat = WeekdayMask.EveryDay)
        val la = ZoneId.of("America/Los_Angeles")
        val tokyo = ZoneId.of("Asia/Tokyo")
        val now = at(tokyo, 2026, 5, 1, 3, 0)
        val next = AlarmMath.nextTrigger(alarm, now, la)
        assertNotNull(next)
        val laLocal = java.time.Instant.ofEpochMilli(next!!).atZone(la).toLocalTime()
        assertEquals(6, laLocal.hour)
    }

    @Test
    fun `snooze target is clamped to sane bounds`() {
        val now = 1_000_000L
        assertEquals(now + 60_000L, AlarmMath.snoozeTarget(now, 0))
        assertEquals(now + 5 * 60_000L, AlarmMath.snoozeTarget(now, 5))
        assertEquals(now + 1440 * 60_000L, AlarmMath.snoozeTarget(now, 100_000))
    }

    @Test
    fun `request codes are deterministic and non negative`() {
        val a = AlarmMath.requestCode(42, 1_700_000_000_000L)
        val b = AlarmMath.requestCode(42, 1_700_000_000_000L)
        val c = AlarmMath.requestCode(43, 1_700_000_000_000L)
        assertEquals(a, b)
        assertTrue(a >= 0)
        assertTrue(c >= 0)
        assertTrue("different alarms should differ", a != c)
    }

    @Test
    fun `request codes differ for different occurrences of the same alarm`() {
        val first = AlarmMath.requestCode(7, 1_700_000_000_000L)
        val second = AlarmMath.requestCode(7, 1_700_000_000_000L + 24 * 60 * 60 * 1000L)
        assertTrue(first != second)
    }

    @Test
    fun `grace window accepts recent and rejects stale triggers`() {
        val now = 10_000_000L
        assertTrue(AlarmMath.isWithinGrace(now - 1000, now))
        assertTrue(AlarmMath.isWithinGrace(now, now))
        assertTrue(!AlarmMath.isWithinGrace(now + 5000, now))
        assertTrue("two hours late should be stale", !AlarmMath.isWithinGrace(now - 2 * 60 * 60_000, now))
    }

    @Test
    fun `upcoming triggers lists successive days`() {
        val alarm = Alarm(hour = 8, minute = 0, repeat = WeekdayMask.EveryDay)
        val now = at(utc, 2026, 3, 10, 0, 0)
        val list = AlarmMath.upcomingTriggers(alarm, now, count = 3, zone = utc)
        assertEquals(3, list.size)
        assertTrue(list[1] - list[0] >= 23 * 60 * 60_000L)
        assertTrue(list[2] > list[1])
    }

    @Test
    fun `month and year rollover works`() {
        val alarm = Alarm(hour = 7, minute = 0, repeat = WeekdayMask.EveryDay)
        val now = at(utc, 2026, 12, 31, 23, 30)
        assertEquals(at(utc, 2027, 1, 1, 7, 0), AlarmMath.nextTrigger(alarm, now, utc))
    }

    @Test
    fun `leap day is handled`() {
        val alarm = Alarm(hour = 6, minute = 0, repeat = WeekdayMask.EveryDay)
        val now = at(utc, 2028, 2, 28, 23, 30)
        assertEquals(at(utc, 2028, 2, 29, 6, 0), AlarmMath.nextTrigger(alarm, now, utc))
    }

    @Test
    fun `ring timeout is bounded`() {
        assertTrue(AlarmMath.ringTimeoutMillis(Alarm(kind = AlarmKind.Timer)) <= 5 * 60_000L)
        assertTrue(AlarmMath.ringTimeoutMillis(Alarm()) <= 15 * 60_000L)
    }
}
