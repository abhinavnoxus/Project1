package com.nexora.app.data.repo

import com.nexora.app.data.entity.AlarmEntity
import com.nexora.app.data.entity.AlarmInstanceEntity
import com.nexora.app.data.entity.FocusSessionEntity
import com.nexora.app.data.entity.RoutineEntity
import com.nexora.app.data.entity.TaskEntity
import com.nexora.app.domain.alarm.AlarmInstance
import com.nexora.app.domain.alarm.AlarmPhase
import com.nexora.app.domain.model.Alarm
import com.nexora.app.domain.model.AlarmKind
import com.nexora.app.domain.model.FocusSession
import com.nexora.app.domain.model.Routine
import com.nexora.app.domain.model.Task
import com.nexora.app.domain.model.TaskKind
import com.nexora.app.domain.model.WeekdayMask

fun AlarmEntity.toDomain(): Alarm = Alarm(
    id = id,
    label = label,
    hour = hour,
    minute = minute,
    repeat = WeekdayMask(repeatBits),
    enabled = enabled,
    ringtoneUri = ringtoneUri,
    vibrate = vibrate,
    snoozeMinutes = snoozeMinutes,
    maxSnoozes = maxSnoozes,
    fadeInSeconds = fadeInSeconds,
    kind = runCatching { AlarmKind.valueOf(kind) }.getOrDefault(AlarmKind.Repeating),
    oneShotAt = oneShotAt,
    skipNext = skipNext,
    createdAt = createdAt,
)

fun Alarm.toEntity(): AlarmEntity = AlarmEntity(
    id = id,
    label = label,
    hour = hour,
    minute = minute,
    repeatBits = repeat.bits,
    enabled = enabled,
    ringtoneUri = ringtoneUri,
    vibrate = vibrate,
    snoozeMinutes = snoozeMinutes,
    maxSnoozes = maxSnoozes,
    fadeInSeconds = fadeInSeconds,
    kind = kind.name,
    oneShotAt = oneShotAt,
    skipNext = skipNext,
    createdAt = createdAt,
)

fun AlarmInstanceEntity.toDomain(): AlarmInstance = AlarmInstance(
    instanceId = instanceId,
    alarmId = alarmId,
    triggerAt = triggerAt,
    phase = runCatching { AlarmPhase.valueOf(phase) }.getOrDefault(AlarmPhase.Scheduled),
    firedLatencyMillis = firedLatencyMillis,
    snoozeUntil = snoozeUntil,
    snoozeCount = snoozeCount,
)

fun AlarmInstance.toEntity(now: Long = System.currentTimeMillis()): AlarmInstanceEntity = AlarmInstanceEntity(
    instanceId = instanceId,
    alarmId = alarmId,
    triggerAt = triggerAt,
    phase = phase.name,
    firedLatencyMillis = firedLatencyMillis,
    snoozeUntil = snoozeUntil,
    snoozeCount = snoozeCount,
    updatedAt = now,
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    note = note,
    kind = runCatching { TaskKind.valueOf(kind) }.getOrDefault(TaskKind.Task),
    dueAt = dueAt,
    repeat = WeekdayMask(repeatBits),
    priority = priority,
    done = done,
    completedAt = completedAt,
    reminderScheduled = reminderScheduled,
    createdAt = createdAt,
    sortIndex = sortIndex,
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    note = note,
    kind = kind.name,
    dueAt = dueAt,
    repeatBits = repeat.bits,
    priority = priority,
    done = done,
    completedAt = completedAt,
    reminderScheduled = reminderScheduled,
    createdAt = createdAt,
    sortIndex = sortIndex,
)

fun RoutineEntity.toDomain(): Routine = Routine(
    id = id,
    name = name,
    start = start,
    durationMinutes = durationMinutes,
    repeat = WeekdayMask(repeatBits),
    colorIndex = colorIndex,
    enabled = enabled,
    createdAt = createdAt,
)

fun Routine.toEntity(): RoutineEntity = RoutineEntity(
    id = id,
    name = name,
    start = start,
    durationMinutes = durationMinutes,
    repeatBits = repeat.bits,
    colorIndex = colorIndex,
    enabled = enabled,
    createdAt = createdAt,
)

fun FocusSessionEntity.toDomain(): FocusSession = FocusSession(
    id = id,
    startedAt = startedAt,
    endedAt = endedAt,
    plannedMinutes = plannedMinutes,
    completed = completed,
    interrupted = interrupted,
    label = label,
)

fun FocusSession.toEntity(): FocusSessionEntity = FocusSessionEntity(
    id = id,
    startedAt = startedAt,
    endedAt = endedAt,
    plannedMinutes = plannedMinutes,
    completed = completed,
    interrupted = interrupted,
    label = label,
)
