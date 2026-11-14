package com.nexora.app.domain.ai

import com.nexora.app.domain.model.Alarm
import com.nexora.app.domain.model.Routine
import com.nexora.app.domain.model.ScheduleEvent
import com.nexora.app.domain.model.Task
import com.nexora.app.domain.model.TaskKind
import com.nexora.app.domain.model.WeekdayMask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BriefingTest {

    private val zone = ZoneId.of("UTC")
    private val day = LocalDate.of(2026, 3, 10) // Tuesday
    private val nineAm = day.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()

    @Test fun `empty data produces honest facts not invented content`() {
        val briefing = Briefing.dailyBriefing(
            date = day, nowMillis = nineAm, events = emptyList(), alarms = emptyList(),
            priorities = emptyList(), reminders = emptyList(), routines = emptyList(),
            completedRoutines = emptySet(), focusMinutesToday = 0L, zone = zone,
        )
        assertTrue(briefing.lines.any { it.text.contains("No calendar events") })
        assertTrue(briefing.lines.any { it.text.contains("No enabled alarms") })
        assertTrue(briefing.facts.isNotEmpty())
        // A suggestion is allowed, but it must be labelled as one.
        briefing.suggestions.forEach { assertEquals(Briefing.InsightKind.Suggestion, it.kind) }
    }

    @Test fun `conflicts are reported and never resolved`() {
        val a = ScheduleEvent(1, "Standup", nineAm, nineAm + 3_600_000, false, null, null, null)
        val b = ScheduleEvent(2, "Review", nineAm + 1_800_000, nineAm + 5_400_000, false, null, null, null)
        val briefing = Briefing.dailyBriefing(
            day, nineAm, listOf(a, b), emptyList(), emptyList(), emptyList(),
            emptyList(), emptySet(), 0L, zone,
        )
        assertEquals(1, briefing.conflicts)
        assertTrue(briefing.lines.any { it.text.contains("overlapping") })
        assertTrue("must state nothing was changed", briefing.lines.any { it.text.contains("does not reschedule") })
    }

    @Test fun `next alarm is reported as a fact`() {
        val alarm = Alarm(hour = 18, minute = 30, repeat = WeekdayMask.EveryDay, label = "Gym")
        val briefing = Briefing.dailyBriefing(
            day, nineAm, emptyList(), listOf(alarm), emptyList(), emptyList(),
            emptyList(), emptySet(), 0L, zone,
        )
        val line = briefing.lines.first { it.text.contains("Next alarm") }
        assertEquals(Briefing.InsightKind.Fact, line.kind)
        assertTrue(line.text.contains("Gym"))
    }

    @Test fun `overdue reminders are surfaced from real due dates`() {
        val overdue = Task(id = 1, title = "Pay bill", kind = TaskKind.Reminder, dueAt = nineAm - 3_600_000)
        val briefing = Briefing.dailyBriefing(
            day, nineAm, emptyList(), emptyList(), emptyList(), listOf(overdue),
            emptyList(), emptySet(), 0L, zone,
        )
        assertTrue(briefing.lines.any { it.text.contains("Pay bill") && it.kind == Briefing.InsightKind.Fact })
    }

    @Test fun `routine progress counts only today's due routines`() {
        val routine = Routine(id = 1, name = "Meditate", start = 7 * 60, repeat = WeekdayMask.EveryDay)
        val briefing = Briefing.dailyBriefing(
            day, nineAm, emptyList(), emptyList(), emptyList(), emptyList(),
            listOf(routine), setOf(routine.id), 20L, zone,
        )
        assertTrue(briefing.lines.any { it.text.contains("1 of 1 routines kept") })
        assertTrue(briefing.lines.any { it.text.contains("20 minutes of focus") })
    }

    @Test fun `every line is classified`() {
        val briefing = Briefing.dailyBriefing(
            day, nineAm, emptyList(), emptyList(),
            listOf(Task(id = 1, title = "Ship", kind = TaskKind.Priority, priority = 1)),
            emptyList(), emptyList(), emptySet(), 0L, zone,
        )
        assertTrue(briefing.lines.all { it.kind in Briefing.InsightKind.entries })
    }
}
