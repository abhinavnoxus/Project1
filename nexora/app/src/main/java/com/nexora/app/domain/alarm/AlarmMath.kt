package com.nexora.app.domain.alarm

import com.nexora.app.domain.model.Alarm
import com.nexora.app.domain.model.AlarmKind
import com.nexora.app.domain.model.TimeOfDay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure scheduling arithmetic for alarms.
 *
 * Everything here is a function of (alarm, now, zone). No Android types, no I/O, no
 * clocks captured statically — so every rule below is unit tested, including the
 * daylight-saving and time-zone-change cases that usually break alarm apps.
 */
object AlarmMath {

    /**
     * The next instant this alarm should fire after [nowMillis], or null when the alarm
     * can never fire again (disabled, or a one-shot whose moment has passed).
     */
    fun nextTrigger(alarm: Alarm, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (!alarm.enabled) return null

        if (alarm.kind == AlarmKind.OneShot) {
            val at = alarm.oneShotAt ?: return null
            return at.takeIf { it > nowMillis }
        }

        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
        val time = alarm.time

        if (alarm.repeat.isEmpty) {
            // "Once" alarm expressed as a wall-clock time: next occurrence of that time.
            val today = atTime(now.toLocalDate(), time, zone)
            return if (today.toInstant().toEpochMilli() > nowMillis) {
                today.toInstant().toEpochMilli()
            } else {
                atTime(now.toLocalDate().plusDays(1), time, zone).toInstant().toEpochMilli()
            }
        }

        // Repeating: scan forward at most 8 days (a full week + today).
        var date = now.toLocalDate()
        for (i in 0..7) {
            val candidateDate = date.plusDays(i.toLong())
            if (!alarm.repeat.contains(candidateDate.dayOfWeek)) continue
            val candidate = atTime(candidateDate, time, zone)
            if (candidate.toInstant().toEpochMilli() > nowMillis) {
                return candidate.toInstant().toEpochMilli()
            }
        }
        return null
    }

    /**
     * Resolves a wall-clock time on a date, handling the two DST edge cases:
     * a time that does not exist (spring forward) is pushed to the gap's end; a time that
     * occurs twice (fall back) resolves to the earlier occurrence.
     */
    fun atTime(date: LocalDate, time: TimeOfDay, zone: ZoneId): ZonedDateTime {
        val local = LocalDateTime.of(date, java.time.LocalTime.of(time.hour, time.minute))
        val resolved = local.atZone(zone)
        return if (resolved.toLocalDate() != date || resolved.hour != time.hour || resolved.minute != time.minute) {
            // Inside a DST gap — the offset shifted the wall clock. Walk forward to the
            // first instant whose local time is >= the requested time.
            var candidate = resolved
            var guard = 0
            while ((candidate.hour < time.hour || (candidate.hour == time.hour && candidate.minute < time.minute))
                && guard < 180
            ) {
                candidate = resolved.plusMinutes(guard.toLong())
                guard++
            }
            candidate
        } else {
            resolved
        }
    }

    /** Instants after [nowMillis] for the next [count] occurrences — used by the UI preview. */
    fun upcomingTriggers(
        alarm: Alarm,
        nowMillis: Long,
        count: Int = 3,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Long> {
        val out = mutableListOf<Long>()
        var cursor = nowMillis
        repeat(count) {
            val next = nextTrigger(alarm, cursor, zone) ?: return@repeat
            out += next
            cursor = next + 60_000L
        }
        return out
    }

    /**
     * Milliseconds to add when snoozing. Clamped to at least one minute and at most one
     * day so a corrupted preference can never park an alarm in the far future.
     */
    fun snoozeTarget(nowMillis: Long, snoozeMinutes: Int): Long {
        val minutes = snoozeMinutes.coerceIn(1, 1440)
        return nowMillis + minutes * 60_000L
    }

    /**
     * How long an alarm may stay ringing before the OS-level safety timeout stops it.
     * Prevents a stuck wake lock from flattening the battery.
     */
    fun ringTimeoutMillis(alarm: Alarm): Long = if (alarm.kind == AlarmKind.Timer) {
        60_000L * 2
    } else {
        60_000L * 10
    }

    /** Stable, non-negative request code for an alarm instance's PendingIntents. */
    fun requestCode(alarmId: Long, occurrence: Long): Int {
        val mixed = (alarmId * 31 + (occurrence / 60_000L)) and 0x7FFFFFFF
        return mixed.toInt()
    }

    /** Whether a missed trigger should still fire (within the grace window). */
    fun isWithinGrace(triggerAt: Long, nowMillis: Long, graceMinutes: Int = 30): Boolean =
        nowMillis - triggerAt in 0..(graceMinutes * 60_000L)
}
