package com.nexora.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.nexora.app.data.entity.AlarmEntity
import com.nexora.app.data.entity.AlarmInstanceEntity
import com.nexora.app.data.entity.FocusSessionEntity
import com.nexora.app.data.entity.RoutineEntity
import com.nexora.app.data.entity.RoutineLogEntity
import com.nexora.app.data.entity.TaskEntity
import com.nexora.app.data.entity.TimerHistoryEntity
import com.nexora.app.data.entity.WorldClockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    suspend fun getAll(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getEnabled(): List<AlarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}

@Dao
interface AlarmInstanceDao {
    @Query("SELECT * FROM alarm_instances")
    fun observeAll(): Flow<List<AlarmInstanceEntity>>

    @Query("SELECT * FROM alarm_instances WHERE phase = 'Ringing' ORDER BY triggerAt")
    suspend fun ringing(): List<AlarmInstanceEntity>

    @Query("SELECT * FROM alarm_instances WHERE instanceId = :id")
    suspend fun byId(id: String): AlarmInstanceEntity?

    @Upsert
    suspend fun upsert(instance: AlarmInstanceEntity)

    @Query("UPDATE alarm_instances SET phase = :phase, updatedAt = :now WHERE instanceId = :id")
    suspend fun setPhase(id: String, phase: String, now: Long)

    @Query("DELETE FROM alarm_instances WHERE updatedAt < :cutoff AND phase IN ('Dismissed','Expired','Superseded')")
    suspend fun pruneFinished(cutoff: Long)

    @Query("DELETE FROM alarm_instances WHERE phase = 'Ringing' AND triggerAt < :cutoff")
    suspend fun pruneStaleRinging(cutoff: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY done ASC, priority DESC, dueAt IS NULL, dueAt ASC, createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueAt BETWEEN :from AND :to AND done = 0 ORDER BY dueAt")
    suspend fun dueBetween(from: Long, to: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun byId(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET done = :done, completedAt = :completedAt WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, completedAt: Long?)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM tasks WHERE done = 0")
    fun observeOpenCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE done = 1 AND completedAt >= :since")
    suspend fun completedSince(since: Long): Int
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY start")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun byId(id: Long): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(routine: RoutineEntity): Long

    @Update
    suspend fun update(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun logCompletion(log: RoutineLogEntity): Long

    @Query("SELECT * FROM routine_logs WHERE dayEpoch = :epochDay")
    fun observeLogs(epochDay: Long): Flow<List<RoutineLogEntity>>

    @Query("SELECT * FROM routine_logs WHERE routineId = :routineId AND dayEpoch >= :fromEpochDay")
    suspend fun logsSince(routineId: Long, fromEpochDay: Long): List<RoutineLogEntity>

    @Query("SELECT * FROM routine_logs")
    suspend fun allLogs(): List<RoutineLogEntity>
}

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE startedAt >= :since ORDER BY startedAt")
    suspend fun since(since: Long): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt")
    suspend fun all(): List<FocusSessionEntity>

    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    @Query("UPDATE focus_sessions SET plannedMinutes = :minutes WHERE id = :id")
    suspend fun setPlanned(id: Long, minutes: Int)

    @Query("UPDATE focus_sessions SET endedAt = :endedAt, completed = :completed, interrupted = :interrupted WHERE id = :id")
    suspend fun finish(id: Long, endedAt: Long, completed: Boolean, interrupted: Boolean)

    @Query("SELECT * FROM focus_sessions WHERE endedAt IS NULL")
    suspend fun openSessions(): List<FocusSessionEntity>
}

@Dao
interface ExtraDao {
    @Query("SELECT * FROM world_clocks ORDER BY sortIndex")
    fun observeWorldClocks(): Flow<List<WorldClockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWorldClock(clock: WorldClockEntity): Long

    @Query("DELETE FROM world_clocks WHERE id = :id")
    suspend fun deleteWorldClock(id: Long)

    @Query("SELECT * FROM timer_history ORDER BY finishedAt DESC LIMIT 20")
    fun observeTimerHistory(): Flow<List<TimerHistoryEntity>>

    @Insert
    suspend fun addTimerHistory(entry: TimerHistoryEntity): Long
}
