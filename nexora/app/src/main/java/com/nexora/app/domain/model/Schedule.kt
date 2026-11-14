package com.nexora.app.domain.model

/**
 * A calendar event projected into NEXORA's own model. Deliberately independent of the
 * platform CalendarContract so the UI and analytics never depend on Android types.
 */
data class ScheduleEvent(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val location: String?,
    val calendarName: String?,
    val calendarColor: Int?,
) {
    val durationMinutes: Long get() = ((endMillis - startMillis) / 60_000L).coerceAtLeast(0)
}

/** A conflict between two events: reported, never silently resolved. */
data class EventConflict(
    val first: ScheduleEvent,
    val second: ScheduleEvent,
) {
    val overlapMinutes: Long
        get() = ((minOf(first.endMillis, second.endMillis) - maxOf(first.startMillis, second.startMillis)) / 60_000L)
            .coerceAtLeast(0)
}
