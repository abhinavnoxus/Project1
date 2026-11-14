package com.nexora.app.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.nexora.app.domain.model.EventConflict
import com.nexora.app.domain.model.ScheduleEvent
import java.time.LocalDate
import java.time.ZoneId

/**
 * Read-only calendar projection.
 *
 * NEXORA only ever *reads*. There is no insert/update/delete code path against the
 * calendar provider anywhere in the app — that is the strongest possible guarantee that
 * NEXORA cannot silently change your schedule.
 */
object CalendarBridge {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun todayEvents(context: Context, date: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): List<ScheduleEvent> {
        if (!hasPermission(context)) return emptyList()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return eventsBetween(context, start, end)
    }

    fun eventsBetween(context: Context, startMillis: Long, endMillis: Long): List<ScheduleEvent> {
        if (!hasPermission(context)) return emptyList()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.CALENDAR_COLOR,
        )
        val out = mutableListOf<ScheduleEvent>()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(startMillis.toString())
            .appendPath(endMillis.toString())
            .build()
        runCatching {
            context.contentResolver.query(
                uri, projection, null, null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    out += ScheduleEvent(
                        id = cursor.getLong(0),
                        title = cursor.getString(1)?.ifBlank { null } ?: "(No title)",
                        startMillis = cursor.getLong(2),
                        endMillis = cursor.getLong(3),
                        allDay = cursor.getInt(4) == 1,
                        location = cursor.getString(5),
                        calendarName = cursor.getString(6),
                        calendarColor = runCatching { cursor.getInt(7) }.getOrNull(),
                    )
                }
            }
        }
        return out.sortedBy { it.startMillis }
    }

    /** Overlapping pairs of timed (non-all-day) events. Reported to the user, never auto-fixed. */
    fun conflicts(events: List<ScheduleEvent>): List<EventConflict> {
        val timed = events.filterNot { it.allDay }.sortedBy { it.startMillis }
        val out = mutableListOf<EventConflict>()
        for (i in timed.indices) {
            for (j in i + 1 until timed.size) {
                if (timed[j].startMillis >= timed[i].endMillis) break
                if (timed[j].startMillis < timed[i].endMillis) out += EventConflict(timed[i], timed[j])
            }
        }
        return out
    }
}
