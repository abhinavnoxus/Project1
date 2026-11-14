package com.nexora.app.data.repo

import com.nexora.app.data.NexoraDatabase
import com.nexora.app.domain.model.FocusSession
import com.nexora.app.domain.model.Routine
import com.nexora.app.domain.model.Task
import com.nexora.app.domain.model.TaskKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TaskRepository(private val db: NexoraDatabase) {
    fun observeAll(): Flow<List<Task>> = db.taskDao().observeAll().map { l -> l.map { it.toDomain() } }

    fun observePriorities(): Flow<List<Task>> = observeAll().map { tasks ->
        tasks.filter { it.kind == TaskKind.Priority && !it.done }.sortedByDescending { it.priority }
    }

    suspend fun dueBetween(from: Long, to: Long): List<Task> =
        db.taskDao().dueBetween(from, to).map { it.toDomain() }

    suspend fun save(task: Task): Long = if (task.id == 0L) {
        db.taskDao().insert(task.toEntity())
    } else {
        db.taskDao().update(task.toEntity()); task.id
    }

    suspend fun setDone(id: Long, done: Boolean) =
        db.taskDao().setDone(id, done, if (done) System.currentTimeMillis() else null)

    suspend fun delete(id: Long) = db.taskDao().delete(id)

    suspend fun completedSince(since: Long): Int = db.taskDao().completedSince(since)
}

class RoutineRepository(private val db: NexoraDatabase) {
    fun observeAll(): Flow<List<Routine>> = db.routineDao().observeAll().map { l -> l.map { it.toDomain() } }

    fun observeLogs(day: LocalDate): Flow<Set<Long>> =
        db.routineDao().observeLogs(day.toEpochDay()).map { logs -> logs.map { it.routineId }.toSet() }

    suspend fun save(routine: Routine): Long = if (routine.id == 0L) {
        db.routineDao().insert(routine.toEntity())
    } else {
        db.routineDao().update(routine.toEntity()); routine.id
    }

    suspend fun delete(id: Long) = db.routineDao().delete(id)

    /** Idempotent: completing twice on the same day never creates a second row. */
    suspend fun markComplete(routineId: Long, day: LocalDate) {
        db.routineDao().logCompletion(
            com.nexora.app.data.entity.RoutineLogEntity(
                routineId = routineId,
                dayEpoch = day.toEpochDay(),
                completedAt = System.currentTimeMillis(),
            )
        )
    }

    /** Streak length for every routine, in one pass over the log table. */
    suspend fun streaks(today: LocalDate): Map<Long, Int> {
        val logs = db.routineDao().allLogs()
        val byRoutine = logs.groupBy { it.routineId }.mapValues { (_, rows) -> rows.map { it.dayEpoch }.toSet() }
        return byRoutine.mapValues { (_, days) ->
            var streak = 0
            var cursor = today
            while (days.contains(cursor.toEpochDay())) {
                streak++
                cursor = cursor.minusDays(1)
                if (streak > 1000) break
            }
            streak
        }
    }

    suspend fun completionStreak(routineId: Long, today: LocalDate): Int {
        val logs = db.routineDao().logsSince(routineId, today.toEpochDay() - 120).map { it.dayEpoch }.toSet()
        var streak = 0
        var cursor = today
        while (logs.contains(cursor.toEpochDay())) {
            streak++
            cursor = cursor.minusDays(1)
            if (streak > 400) break
        }
        return streak
    }
}

class FocusRepository(private val db: NexoraDatabase) {
    fun observeRecent(limit: Int = 20): Flow<List<FocusSession>> =
        db.focusDao().observeRecent(limit).map { l -> l.map { it.toDomain() } }

    suspend fun since(millis: Long): List<FocusSession> =
        db.focusDao().since(millis).map { it.toDomain() }

    suspend fun all(): List<FocusSession> = db.focusDao().all().map { it.toDomain() }

    suspend fun start(plannedMinutes: Int, label: String): Long =
        db.focusDao().insert(
            FocusSession(startedAt = System.currentTimeMillis(), plannedMinutes = plannedMinutes, label = label).toEntity()
        )

    suspend fun finish(id: Long, completed: Boolean, interrupted: Boolean) =
        db.focusDao().finish(id, System.currentTimeMillis(), completed, interrupted)

    /** Any session left open by a process death is closed honestly as interrupted. */
    suspend fun closeOrphans() {
        db.focusDao().openSessions().forEach {
            db.focusDao().finish(it.id, it.startedAt + it.plannedMinutes * 60_000L, false, true)
        }
    }
}
