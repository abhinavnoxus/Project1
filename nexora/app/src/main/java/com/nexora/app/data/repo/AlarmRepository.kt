package com.nexora.app.data.repo

import com.nexora.app.data.NexoraDatabase
import com.nexora.app.domain.model.Alarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The single writer for alarms. Scheduling side effects are delegated to
 * [AlarmSchedulerPort] so this class stays testable with a fake scheduler.
 */
class AlarmRepository(
    private val db: NexoraDatabase,
    private val scheduler: AlarmSchedulerPort,
) {
    fun observeAlarms(): Flow<List<Alarm>> =
        db.alarmDao().observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun all(): List<Alarm> = db.alarmDao().getAll().map { it.toDomain() }

    suspend fun alarms(): List<Alarm> = all()

    suspend fun get(id: Long): Alarm? = db.alarmDao().getById(id)?.toDomain()

    /**
     * Persists and reschedules in one call. Returns the stored id (new rows get one from
     * SQLite). The scheduler is always asked to reschedule the whole alarm, never a
     * single occurrence, so a stale occurrence can never survive an edit.
     */
    suspend fun save(alarm: Alarm): Long {
        val id = if (alarm.id == 0L) {
            db.alarmDao().insert(alarm.toEntity())
        } else {
            db.alarmDao().update(alarm.toEntity())
            alarm.id
        }
        val stored = alarm.copy(id = id)
        scheduler.reschedule(stored)
        return id
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        db.alarmDao().setEnabled(id, enabled)
        scheduler.cancelAll(id)
        if (enabled) {
            get(id)?.let { scheduler.reschedule(it) }
        }
    }

    suspend fun delete(id: Long) {
        scheduler.cancelAll(id)
        db.alarmDao().delete(id)
    }

    /** Called after a dismiss so repeating alarms line up their next occurrence. */
    suspend fun rescheduleAll() {
        db.alarmDao().getEnabled().forEach { scheduler.reschedule(it.toDomain()) }
    }
}

/** Port implemented by the Android alarm layer; faked in tests. */
interface AlarmSchedulerPort {
    suspend fun reschedule(alarm: Alarm)
    suspend fun cancelAll(alarmId: Long)
    suspend fun nextAlarmMillis(): Long?
}
