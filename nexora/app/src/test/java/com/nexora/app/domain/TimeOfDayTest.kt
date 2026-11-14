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

    @Test fun `rejects out of range minute of day`() {
        var threw = false
        try { TimeOfDay(1500) } catch (e: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }
}
