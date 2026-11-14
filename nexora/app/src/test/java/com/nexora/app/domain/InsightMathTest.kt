package com.nexora.app.domain

import com.nexora.app.domain.insight.FocusStats
import com.nexora.app.domain.insight.InsightMath
import com.nexora.app.domain.model.FocusSession
import com.nexora.app.domain.model.ScheduleEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class InsightMathTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun session(startHour: Int, minutes: Int, day: LocalDate = LocalDate.of(2026, 3, 10)) =
        FocusSession(
            id = 0,
            startedAt = day.atTime(startHour, 0).atZone(zone).toInstant().toEpochMilli(),
            endedAt = day.atTime(startHour, 0).plusMinutes(minutes.toLong())
                .atZone(zone).toInstant().toEpochMilli(),
            plannedMinutes = minutes,
            completed = true,
            label = "Focus",
        )

    @Test fun `total focus sums completed sessions only`() {
        val sessions = listOf(
            session(9, 25),
            session(11, 50),
            session(14, 25).copy(completed = false),
        )
        assertEquals(75L, InsightMath.totalFocusMinutes(sessions))
    }

    @Test fun `per day grouping uses the local zone`() {
        val day1 = LocalDate.of(2026, 3, 10)
        val day2 = LocalDate.of(2026, 3, 11)
        val sessions = listOf(session(9, 60, day1), session(10, 30, day1), session(9, 45, day2))
        val grouped = InsightMath.focusByDay(sessions, zone)
        assertEquals(90L, grouped[day1])
        assertEquals(45L, grouped[day2])
    }

    @Test fun `streak counts consecutive days ending today`() {
        val today = LocalDate.of(2026, 3, 12)
        val map = mapOf(
            today to 30L,
            today.minusDays(1) to 25L,
            today.minusDays(2) to 10L,
            today.minusDays(4) to 60L,
        )
        assertEquals(3, InsightMath.streak(map, today))
    }

    @Test fun `empty history produces zeroes not fabrications`() {
        val stats = InsightMath.summarise(emptyList(), zone)
        assertEquals(0L, stats.totalMinutes)
        assertEquals(0, stats.sessionCount)
        assertEquals(0L, stats.longestSessionMinutes)
    }

    @Test fun `schedule density is computed from real events`() {
        val day = LocalDate.of(2026, 3, 10)
        val events = listOf(
            ScheduleEvent(1, "A", day.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(), day.atTime(10, 0).atZone(zone).toInstant().toEpochMilli(), false, null, null, null),
            ScheduleEvent(2, "B", day.atTime(11, 0).atZone(zone).toInstant().toEpochMilli(), day.atTime(12, 30).atZone(zone).toInstant().toEpochMilli(), false, null, null, null),
        )
        assertEquals(150L, InsightMath.busyMinutes(events))
        val density = InsightMath.density(events, day, zone, dayStartHour = 8, dayEndHour = 20)
        assertTrue(density > 0.2f && density <= 1f)
    }

    @Test fun `all day events do not count toward density`() {
        val day = LocalDate.of(2026, 3, 10)
        val millis = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val events = listOf(ScheduleEvent(1, "Holiday", millis, millis + 86_400_000, true, null, null, null))
        assertEquals(0L, InsightMath.busyMinutes(events))
    }

    @Test fun `routine consistency is measured only from logged days`() {
        assertEquals(0f, InsightMath.consistency(completed = 0, scheduled = 0))
        assertEquals(1f, InsightMath.consistency(completed = 5, scheduled = 5))
        assertEquals(0.5f, InsightMath.consistency(completed = 5, scheduled = 10))
    }

    @Test fun `stats carry a readable summary`() {
        val sessions = listOf(session(9, 25), session(10, 50))
        val stats = InsightMath.summarise(sessions, zone)
        assertEquals(2, stats.sessionCount)
        assertEquals(75L, stats.totalMinutes)
        assertEquals(50L, stats.longestSessionMinutes)
        assertTrue(stats.averageMinutes > 0)
    }
}
