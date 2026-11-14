package com.nexora.app.domain.insight

import com.nexora.app.domain.model.FocusSession
import com.nexora.app.domain.model.ScheduleEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * All analytics arithmetic.
 *
 * Everything is derived from records NEXORA actually stored. There is no projection, no
 * extrapolation and no synthetic filler: if there is no data, the answer is zero and the
 * UI says "no sessions yet" rather than inventing a trend.
 */
object InsightMath {

    fun totalFocusMinutes(sessions: List<FocusSession>): Long =
        sessions.filter { it.completed && it.endedAt != null }
            .sumOf { (it.endedAt!! - it.startedAt) / 60_000L }
            .coerceAtLeast(0L)

    fun focusByDay(sessions: List<FocusSession>, zone: ZoneId): Map<LocalDate, Long> {
        val out = LinkedHashMap<LocalDate, Long>()
        sessions.filter { it.completed && it.endedAt != null }.forEach { s ->
            val day = Instant.ofEpochMilli(s.startedAt).atZone(zone).toLocalDate()
            out[day] = (out[day] ?: 0L) + ((s.endedAt!! - s.startedAt) / 60_000L).coerceAtLeast(0L)
        }
        return out
    }

    fun streak(byDay: Map<LocalDate, Long>, today: LocalDate): Int {
        var cursor = today
        var streak = 0
        while ((byDay[cursor] ?: 0L) > 0L) {
            streak++
            cursor = cursor.minusDays(1)
            if (streak > 1000) break
        }
        return streak
    }

    fun summarise(sessions: List<FocusSession>, zone: ZoneId): FocusStats {
        val completed = sessions.filter { it.completed && it.endedAt != null }
        val durations = completed.map { ((it.endedAt!! - it.startedAt) / 60_000L).coerceAtLeast(0L) }
        val total = durations.sum()
        val byDay = focusByDay(sessions, zone)
        return FocusStats(
            totalMinutes = total,
            sessionCount = completed.size,
            longestSessionMinutes = durations.maxOrNull() ?: 0L,
            averageMinutes = if (durations.isEmpty()) 0L else total / durations.size,
            activeDays = byDay.count { it.value > 0L },
            streakDays = streak(byDay, LocalDate.now(zone)),
            byDay = byDay,
        )
    }

    fun busyMinutes(events: List<ScheduleEvent>): Long =
        events.filterNot { it.allDay }.sumOf { it.durationMinutes }.coerceAtLeast(0L)

    /** Fraction of the waking window occupied by real, timed events. */
    fun density(
        events: List<ScheduleEvent>,
        day: LocalDate,
        zone: ZoneId,
        dayStartHour: Int = 7,
        dayEndHour: Int = 22,
    ): Float {
        val windowMinutes = ((dayEndHour - dayStartHour) * 60).coerceAtLeast(1)
        val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val clamped = events.filterNot { it.allDay }.sumOf { e ->
            val start = maxOf(e.startMillis, dayStart + dayStartHour * 3_600_000L)
            val end = minOf(e.endMillis, dayStart + dayEndHour * 3_600_000L)
            ((end - start) / 60_000L).coerceAtLeast(0L)
        }
        return (clamped.toFloat() / windowMinutes).coerceIn(0f, 1f)
    }

    fun consistency(completed: Int, scheduled: Int): Float =
        if (scheduled <= 0) 0f else (completed.toFloat() / scheduled).coerceIn(0f, 1f)

    /** Reminder completion rate over the trailing window. */
    fun completionRate(completed: Int, total: Int): Float =
        if (total <= 0) 0f else (completed.toFloat() / total).coerceIn(0f, 1f)
}

data class FocusStats(
    val totalMinutes: Long,
    val sessionCount: Int,
    val longestSessionMinutes: Long,
    val averageMinutes: Long,
    val activeDays: Int,
    val streakDays: Int,
    val byDay: Map<LocalDate, Long>,
) {
    val totalLabel: String
        get() = when {
            totalMinutes >= 60 -> "${totalMinutes / 60}h ${totalMinutes % 60}m"
            totalMinutes > 0 -> "${totalMinutes}m"
            else -> "—"
        }
}
