package com.nexora.app.domain

import com.nexora.app.domain.model.TimeOfDay
import com.nexora.app.domain.model.WeekdayMask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class TimeOfDayTest {

    @Test fun `formats 12 and 24 hour`() {
        assertEquals("12:00 AM", TimeOfDay.of(0, 0).format12h())
        assertEquals("12:30 PM", TimeOfDay.of(12, 30).format12h())
        assertEquals("07:05", TimeOfDay.of(7, 5).format24h())
        assertEquals("11:59 PM", TimeOfDay.of(23, 59).format12h())
    }

    @Test fun `parses valid and rejects invalid`() {
        assertEquals(TimeOfDay.of(14, 5), TimeOfDay.parse("14:05"))
        assertNull(TimeOfDay.parse("24:00"))
        assertNull(TimeOfDay.parse("7:60"))
        assertNull(TimeOfDay.parse("abc"))
        assertNull(TimeOfDay.parse("7"))
    }

    @Test fun `plus minutes wraps around midnight`() {
        assertEquals(TimeOfDay.of(0, 15), TimeOfDay.of(23, 45).plusMinutes(30))
        assertEquals(TimeOfDay.of(23, 45), TimeOfDay.of(0, 15).plusMinutes(-30))
    }

    @Test fun `circular distance uses the short way`() {
        assertEquals(30, TimeOfDay.of(23, 50).circularDistanceTo(TimeOfDay.of(0, 20)))
        assertEquals(-30, TimeOfDay.of(0, 20).circularDistanceTo(TimeOfDay.of(23, 50)))
        assertEquals(0, TimeOfDay.of(6, 0).circularDistanceTo(TimeOfDay.of(6, 0)))
    }

    @Test fun `weekday mask operations`() {
        val weekdays = WeekdayMask.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        assertTrue(weekdays.contains(DayOfWeek.MONDAY))
        assertTrue(!weekdays.contains(DayOfWeek.SUNDAY))
        assertEquals("Weekdays", weekdays.label())
        assertEquals("Every day", WeekdayMask.EveryDay.label())
        assertEquals("Once", WeekdayMask.None.label())
        val withSunday = weekdays.with(DayOfWeek.SUNDAY)
        assertTrue(withSunday.contains(DayOfWeek.SUNDAY))
        val without = withSunday.with(DayOfWeek.SUNDAY, on = false)
        assertTrue(!without.contains(DayOfWeek.SUNDAY))
    }

    @Test fun `every day mask covers all seven days including sunday`() {
        java.time.DayOfWeek.entries.forEach { day ->
            assertTrue("EveryDay must contain $day", WeekdayMask.EveryDay.contains(day))
        }
        assertEquals(127, WeekdayMask.EveryDay.bits)
        assertEquals(1 shl 6, WeekdayMask.of(java.time.DayOfWeek.SUNDAY).bits)
        assertEquals(1, WeekdayMask.of(java.time.DayOfWeek.MONDAY).bits)
    }

    @Test fun `sunday alarm actually schedules`() {
        val alarm = com.nexora.app.domain.model.Alarm(
            hour = 8, minute = 0,
            repeat = WeekdayMask.of(java.time.DayOfWeek.SUNDAY),
        )
        val zone = java.time.ZoneId.of("UTC")
        // Wednesday 2026-03-11 -> the next Sunday is 2026-03-15.
        val now = java.time.ZonedDateTime.of(2026, 3, 11, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val next = com.nexora.app.domain.alarm.AlarmMath.nextTrigger(alarm, now, zone)
        val local = java.time.Instant.ofEpochMilli(next!!).atZone(zone)
        assertEquals(15, local.dayOfMonth)
        assertEquals(java.time.DayOfWeek.SUNDAY, local.dayOfWeek)
    }

    @Test fun `weekend preset includes both saturday and sunday`() {
        val weekend = WeekdayMask.of(java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY)
        assertEquals("Weekends", weekend.label())
        assertTrue(weekend.contains(java.time.DayOfWeek.SATURDAY))
        assertTrue(weekend.contains(java.time.DayOfWeek.SUNDAY))
        assertTrue(!weekend.contains(java.time.DayOfWeek.FRIDAY))
    }

    @Test fun `rejects out of range minute of day`() {
        var threw = false
        try { TimeOfDay(1500) } catch (e: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }
}
