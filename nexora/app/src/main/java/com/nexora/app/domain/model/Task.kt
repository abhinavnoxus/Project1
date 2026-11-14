package com.nexora.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TaskKind { Task, Reminder, Priority }

@Serializable
data class Task(
    val id: Long = 0L,
    val title: String = "",
    val note: String = "",
    val kind: TaskKind = TaskKind.Task,
    val dueAt: Long? = null,
    val repeat: WeekdayMask = WeekdayMask.None,
    val priority: Int = 0,
    val done: Boolean = false,
    val completedAt: Long? = null,
    /** Set when a reminder notification has been scheduled for [dueAt]. */
    val reminderScheduled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val sortIndex: Int = 0,
)

@Serializable
data class Routine(
    val id: Long = 0L,
    val name: String = "",
    val start: Int = 7 * 60,
    val durationMinutes: Int = 20,
    val repeat: WeekdayMask = WeekdayMask.EveryDay,
    val colorIndex: Int = 0,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class FocusSession(
    val id: Long = 0L,
    val startedAt: Long,
    val endedAt: Long? = null,
    val plannedMinutes: Int = 25,
    val completed: Boolean = false,
    val interrupted: Boolean = false,
    val label: String = "Focus",
) {
    val actualMillis: Long get() = (endedAt ?: System.currentTimeMillis()) - startedAt
}

@Serializable
data class WorldClock(
    val id: Long = 0L,
    val zoneId: String = "Asia/Kolkata",
    val label: String = "Local",
    val sortIndex: Int = 0,
)
