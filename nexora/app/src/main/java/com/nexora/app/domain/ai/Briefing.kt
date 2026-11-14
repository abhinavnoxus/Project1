package com.nexora.app.domain.ai

import com.nexora.app.domain.model.Alarm
import com.nexora.app.domain.model.Routine
import com.nexora.app.domain.model.ScheduleEvent
import com.nexora.app.domain.model.Task
import com.nexora.app.platform.CalendarBridge
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * NEXORA INTELLIGENCE — the offline briefing engine.
 *
 * Design constraints this file obeys:
 *  1. **Offline only.** There is no network call anywhere in the app, so the briefing is
 *     computed from local rules over local data. No API key, no account, no upload.
 *  2. **No fabrication.** Every sentence is either a fact read from the database, a
 *     deterministic calculation over those facts, or — when it is a recommendation —
 *     explicitly labelled as such with the [InsightKind].
 *  3. **Never mutates anything.** This object returns text and suggestions. Creating,
 *     editing or deleting a task/alarm/event always requires a user tap; no code path
 *     here writes to the database.
 */
object Briefing {

    enum class InsightKind {
        /** Directly read from stored data. */
        Fact,

        /** Deterministic arithmetic on stored data. */
        Derived,

        /** A rule-based suggestion, clearly marked as a suggestion. */
        Suggestion,
    }

    data class Line(
        val text: String,
        val kind: InsightKind,
    )

    data class DailyBriefing(
        val headline: String,
        val lines: List<Line>,
        val conflicts: Int,
        val generatedAt: Long,
    ) {
        val suggestions: List<Line> get() = lines.filter { it.kind == InsightKind.Suggestion }
        val facts: List<Line> get() = lines.filter { it.kind != InsightKind.Suggestion }
    }

    fun dailyBriefing(
        date: LocalDate,
        nowMillis: Long,
        events: List<ScheduleEvent>,
        alarms: List<Alarm>,
        priorities: List<Task>,
        reminders: List<Task>,
        routines: List<Routine>,
        completedRoutines: Set<Long>,
        focusMinutesToday: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DailyBriefing {
        val lines = mutableListOf<Line>()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val timed = events.filterNot { it.allDay }.sortedBy { it.startMillis }
        val conflicts = CalendarBridge.conflicts(events)

        // ---- facts about the calendar
        when {
            events.isEmpty() -> lines += Line(
                "No calendar events today. That is read from your calendar, not assumed.",
                InsightKind.Fact,
            )
            else -> {
                val next = timed.firstOrNull { it.endMillis > nowMillis }
                lines += Line(
                    "${events.size} event${if (events.size == 1) "" else "s"} today" +
                        (next?.let { ", next is \"${it.title}\" at " + Instant.ofEpochMilli(it.startMillis).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm")) } ?: ""),
                    InsightKind.Fact,
                )
                val busy = timed.filterNot { it.allDay }.sumOf { it.durationMinutes }
                lines += Line(
                    formatBusy(busy) + " of timed commitments.",
                    InsightKind.Derived,
                )
            }
        }

        if (conflicts.isNotEmpty()) {
            lines += Line(
                "${conflicts.size} overlapping pair${if (conflicts.size == 1) "" else "s"}: " +
                    conflicts.take(2).joinToString("; ") { "\"${it.first.title}\" ↔ \"${it.second.title}\" (${it.overlapMinutes} min)" } +
                    ". NEXORA does not reschedule anything — this is a report.",
                InsightKind.Fact,
            )
            lines += Line(
                "Consider moving one of the overlapping blocks. Ask before any change is made.",
                InsightKind.Suggestion,
            )
        }

        // ---- alarms
        val nextAlarm = alarms.filter { it.enabled }
            .mapNotNull { alarm -> com.nexora.app.domain.alarm.AlarmMath.nextTrigger(alarm, nowMillis, zone)?.let { alarm to it } }
            .minByOrNull { it.second }
        if (nextAlarm != null) {
            val at = Instant.ofEpochMilli(nextAlarm.second).atZone(zone)
            lines += Line(
                "Next alarm: \"${nextAlarm.first.label.ifBlank { "Alarm" }}\" at ${at.format(DateTimeFormatter.ofPattern("EEE HH:mm"))}.",
                InsightKind.Fact,
            )
        } else {
            lines += Line("No enabled alarms. Nothing will wake you.", InsightKind.Fact)
        }

        // ---- priorities and reminders
        if (priorities.isEmpty()) {
            lines += Line(
                "No priorities set. Choosing up to three is the highest-leverage thing you can do right now.",
                InsightKind.Suggestion,
            )
        } else {
            val done = priorities.count { it.done }
            lines += Line(
                "$done of ${priorities.size} priorities complete.",
                InsightKind.Derived,
            )
            if (done == 0 && now.hour >= 15) {
                lines += Line(
                    "It is after 15:00 and no priority is done yet. Pick the smallest one and start it.",
                    InsightKind.Suggestion,
                )
            }
        }

        val overdue = reminders.filter { task ->
            val due = task.dueAt
            due != null && due < nowMillis && !task.done
        }
        if (overdue.isNotEmpty()) {
            lines += Line(
                "${overdue.size} reminder${if (overdue.size == 1) " is" else "s are"} past due: " +
                    overdue.take(3).joinToString(", ") { "\"${it.title}\"" } + ".",
                InsightKind.Fact,
            )
        }

        // ---- routines
        val dueRoutines = routines.filter { it.enabled && it.repeat.contains(date.dayOfWeek) }
        if (dueRoutines.isNotEmpty()) {
            val kept = dueRoutines.count { completedRoutines.contains(it.id) }
            lines += Line(
                "$kept of ${dueRoutines.size} routines kept today.",
                InsightKind.Derived,
            )
            val upcomingRoutine = dueRoutines
                .filterNot { completedRoutines.contains(it.id) }
                .minByOrNull { kotlin.math.abs(it.start - (now.hour * 60 + now.minute)) }
            if (upcomingRoutine != null) {
                lines += Line(
                    "Closest unkept routine: \"${upcomingRoutine.name}\" at " +
                        com.nexora.app.domain.model.TimeOfDay(upcomingRoutine.start).format12h() + ".",
                    InsightKind.Fact,
                )
            }
        }

        // ---- focus
        if (focusMinutesToday == 0L) {
            val gap = findFocusGap(timed, now, zone)
            lines += if (gap != null) {
                Line(
                    "No focus time logged yet. There is a ${gap.second}-minute window from ${gap.first}.",
                    InsightKind.Suggestion,
                )
            } else {
                Line("No focus time logged yet today.", InsightKind.Derived)
            }
        } else {
            lines += Line("$focusMinutesToday minutes of focus logged today.", InsightKind.Derived)
        }

        return DailyBriefing(
            headline = headlineFor(now.toLocalTime()),
            lines = lines,
            conflicts = conflicts.size,
            generatedAt = nowMillis,
        )
    }

    private fun headlineFor(time: LocalTime): String = when {
        time.hour < 5 -> "Still up — here is where the day stands."
        time.hour < 9 -> "Opening the day."
        time.hour < 12 -> "Morning in progress."
        time.hour < 15 -> "Midday check-in."
        time.hour < 18 -> "Afternoon."
        time.hour < 22 -> "Closing the day."
        else -> "Winding down."
    }

    private fun formatBusy(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            minutes == 0L -> "No"
            h == 0L -> "$minutes minutes"
            m == 0L -> "${h}h"
            else -> "${h}h ${m}m"
        } + " committed today"
    }

    /**
     * Largest gap between now and end of day (22:00) that is at least 25 minutes long.
     * Returns (start label, minutes). Pure arithmetic on real events.
     */
    private fun findFocusGap(
        events: List<ScheduleEvent>,
        now: java.time.ZonedDateTime,
        zone: ZoneId,
    ): Pair<String, Long>? {
        val dayEnd = now.toLocalDate().atTime(22, 0).atZone(zone)
        if (!now.isBefore(dayEnd)) return null
        var cursor = now
        var best: Pair<String, Long>? = null
        for (event in events.sortedBy { it.startMillis }) {
            val start = Instant.ofEpochMilli(event.startMillis).atZone(zone)
            val end = Instant.ofEpochMilli(event.endMillis).atZone(zone)
            if (end.isBefore(now)) continue
            if (start.isAfter(cursor)) {
                val minutes = java.time.Duration.between(cursor, start).toMinutes()
                if (minutes >= 25 && (best == null || minutes > best!!.second)) {
                    best = cursor.format(DateTimeFormatter.ofPattern("HH:mm")) to minutes
                }
            }
            if (end.isAfter(cursor)) cursor = end
        }
        val tail = java.time.Duration.between(cursor, dayEnd).toMinutes()
        if (tail >= 25 && (best == null || tail > best!!.second)) {
            best = cursor.format(DateTimeFormatter.ofPattern("HH:mm")) to tail
        }
        return best
    }
}
