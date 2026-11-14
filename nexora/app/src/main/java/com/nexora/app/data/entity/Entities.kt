package com.nexora.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,
    val hour: Int,
    val minute: Int,
    val repeatBits: Int,
    val enabled: Boolean,
    val ringtoneUri: String?,
    val vibrate: Boolean,
    val snoozeMinutes: Int,
    val maxSnoozes: Int,
    val fadeInSeconds: Int,
    val kind: String,
    val oneShotAt: Long?,
    val skipNext: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "alarm_instances", indices = [Index("alarmId"), Index("triggerAt")])
data class AlarmInstanceEntity(
    @PrimaryKey val instanceId: String,
    val alarmId: Long,
    val triggerAt: Long,
    val phase: String,
    val firedLatencyMillis: Long,
    val snoozeUntil: Long?,
    val snoozeCount: Int,
    val updatedAt: Long,
)

@Entity(tableName = "tasks", indices = [Index("dueAt"), Index("kind")])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val note: String,
    val kind: String,
    val dueAt: Long?,
    val repeatBits: Int,
    val priority: Int,
    val done: Boolean,
    val completedAt: Long?,
    val reminderScheduled: Boolean,
    val createdAt: Long,
    val sortIndex: Int,
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val start: Int,
    val durationMinutes: Int,
    val repeatBits: Int,
    val colorIndex: Int,
    val enabled: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "routine_logs", indices = [Index(value = ["routineId", "dayEpoch"], unique = true)])
data class RoutineLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routineId: Long,
    /** Local date as epoch-day so the record survives time-zone changes. */
    val dayEpoch: Long,
    val completedAt: Long,
)

@Entity(tableName = "focus_sessions", indices = [Index("startedAt")])
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startedAt: Long,
    val endedAt: Long?,
    val plannedMinutes: Int,
    val completed: Boolean,
    val interrupted: Boolean,
    val label: String,
)

@Entity(tableName = "world_clocks")
data class WorldClockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val zoneId: String,
    val label: String,
    val sortIndex: Int,
)

/** Completed countdown timers, for the "recent timers" list. */
@Entity(tableName = "timer_history")
data class TimerHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,
    val durationMillis: Long,
    val finishedAt: Long,
    val cancelled: Boolean,
)
