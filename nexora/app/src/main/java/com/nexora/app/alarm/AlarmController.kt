package com.nexora.app.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.nexora.app.data.NexoraDatabase
import com.nexora.app.notify.NotificationHelper
import com.nexora.app.data.repo.AlarmRepository
import com.nexora.app.data.repo.toDomain
import com.nexora.app.data.repo.toEntity
import com.nexora.app.domain.alarm.AlarmInstance
import com.nexora.app.domain.alarm.AlarmMath
import com.nexora.app.domain.alarm.AlarmPhase
import com.nexora.app.domain.alarm.AlarmStateMachine
import com.nexora.app.domain.alarm.AlarmTransition
import com.nexora.app.domain.model.Alarm
import com.nexora.app.domain.model.AlarmKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZoneId

/**
 * The one place where alarm state changes happen.
 *
 * Why a controller and not "the service does it": the receiver, the notification action,
 * the in-app stop button and the service's own timeout all need to reach the same
 * transitions. Routing them through one object with one mutex means:
 *   - repeated taps are idempotent (state machine reports changed=false),
 *   - the **audio stop is the first thing that happens**, before any Compose work,
 *   - notification cancel + DB write + reschedule always happen together.
 *
 * Nothing in this file touches the UI thread, and nothing in it waits on animation.
 */
object AlarmController {

    private const val TAG = "NexoraAlarm"
    private val mutex = Mutex()

    /** Fast, non-suspending stop used by the notification receiver on the main thread. */
    @Volatile
    private var activeRinger: AlarmRinger? = null

    fun attachRinger(ringer: AlarmRinger?) {
        activeRinger = ringer
    }

    /** Immediately silence whatever is ringing, with no DB or coroutine work in the path. */
    fun silenceNow() {
        activeRinger?.stop()
        activeRinger = null
    }

    private fun repo(context: Context) = AlarmRepository(NexoraDatabase.get(context), AlarmScheduler(context))

    suspend fun fire(context: Context, alarmId: Long, triggerAt: Long) = withContext(Dispatchers.Default) {
        mutex.withLock {
            try {
                val db = NexoraDatabase.get(context)
                val alarmEntity = db.alarmDao().getById(alarmId)
                if (alarmEntity == null) {
                    Log.w(TAG, "fire for unknown alarm $alarmId")
                    return@withLock
                }
                val alarm = alarmEntity.toDomain()
                if (!alarm.enabled) {
                    Log.i(TAG, "alarm $alarmId disabled at fire time; ignoring")
                    return@withLock
                }

                // Grace guard: a stale intent from hours ago must not ring the phone.
                if (!AlarmMath.isWithinGrace(triggerAt, System.currentTimeMillis())) {
                    Log.i(TAG, "stale trigger for alarm $alarmId, ignoring")
                    return@withLock
                }

                val instanceId = instanceIdFor(alarmId, triggerAt)
                val existing = db.alarmInstanceDao().byId(instanceId)?.toDomain()
                if (existing != null && existing.isTerminal) {
                    Log.i(TAG, "instance $instanceId already ${existing.phase}; not re-ringing")
                    return@withLock
                }

                val instance = existing ?: AlarmInstance(
                    instanceId = instanceId,
                    alarmId = alarmId,
                    triggerAt = triggerAt,
                    phase = AlarmPhase.Scheduled,
                )

                when (val transition = AlarmStateMachine.onFired(instance, System.currentTimeMillis())) {
                    is AlarmTransition.Applied -> {
                        val ringing = transition.instance
                        db.alarmInstanceDao().upsert(ringing.toEntity())
                        // Supersede any other ringing instance of a *different* alarm is
                        // deliberately not done: two alarms can legitimately coexist.
                        startService(context, ringing, alarm)
                    }
                    is AlarmTransition.Rejected -> Log.w(TAG, "fire rejected: ${transition.reason}")
                    else -> Unit
                }
            } catch (t: Throwable) {
                Log.e(TAG, "fire failed for alarm $alarmId", t)
            }
        }
    }

    /**
     * Dismiss. Order is deliberate: audio first (the user's intent), then notification,
     * then database, then reschedule. If the process dies after step 1 the alarm is at
     * least silent, and the persisted phase is corrected by [reconcileOnStartup].
     */
    suspend fun dismiss(context: Context, alarmId: Long, triggerAt: Long, source: String) =
        withContext(Dispatchers.Default) {
            silenceNow()
            NotificationHelper.cancelAlert(context, instanceIdFor(alarmId, triggerAt))

            mutex.withLock {
                try {
                    val db = NexoraDatabase.get(context)
                    val instanceId = instanceIdFor(alarmId, triggerAt)
                    val stored = db.alarmInstanceDao().byId(instanceId)?.toDomain()
                    val working = stored ?: AlarmInstance(
                        instanceId = instanceId,
                        alarmId = alarmId,
                        triggerAt = triggerAt,
                        phase = AlarmPhase.Ringing,
                    )
                    when (val t = AlarmStateMachine.onDismissed(working, System.currentTimeMillis())) {
                        is AlarmTransition.Applied -> db.alarmInstanceDao().upsert(t.instance.toEntity())
                        else -> Unit
                    }

                    val repo = repo(context)
                    val alarm = repo.get(alarmId)
                    if (alarm != null) {
                        when (alarm.kind) {
                            AlarmKind.OneShot ->
                                repo.setEnabled(alarmId, false)
                            else -> {
                                // Drop any leftover occurrence record for this instant,
                                // then re-arm for the next one.
                                AlarmScheduler(context).reschedule(alarm)
                            }
                        }
                        AlarmHistory.record(triggerAt, dismissedAt = System.currentTimeMillis(), source = source)
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "dismiss failed for alarm $alarmId", t)
                }
            }
            stopService(context)
        }

    suspend fun snooze(context: Context, alarmId: Long, triggerAt: Long) =
        withContext(Dispatchers.Default) {
            silenceNow()
            NotificationHelper.cancelAlert(context, instanceIdFor(alarmId, triggerAt))

            mutex.withLock {
                try {
                    val db = NexoraDatabase.get(context)
                    val instanceId = instanceIdFor(alarmId, triggerAt)
                    val stored = db.alarmInstanceDao().byId(instanceId)?.toDomain()
                    val working = stored ?: AlarmInstance(
                        instanceId = instanceId,
                        alarmId = alarmId,
                        triggerAt = triggerAt,
                        phase = AlarmPhase.Ringing,
                    )
                    val alarm = repo(context).get(alarmId)
                    val snoozeMinutes = alarm?.snoozeMinutes ?: 10
                    val maxSnoozes = alarm?.maxSnoozes ?: 3

                    when (val t = AlarmStateMachine.onSnooze(working, System.currentTimeMillis(), snoozeMinutes, maxSnoozes)) {
                        is AlarmTransition.Snooze -> {
                            db.alarmInstanceDao().upsert(t.instance.toEntity())
                            val snoozeTarget = t.until
                            val instanceId2 = instanceIdFor(alarmId, snoozeTarget)
                            db.alarmInstanceDao().upsert(
                                AlarmInstance(
                                    instanceId = instanceId2,
                                    alarmId = alarmId,
                                    triggerAt = snoozeTarget,
                                    phase = AlarmPhase.Scheduled,
                                    snoozeCount = t.instance.snoozeCount,
                                ).toEntity()
                            )
                            AlarmScheduler(context).schedule(
                                alarm ?: Alarm(id = alarmId, label = "Alarm"),
                                snoozeTarget,
                            )
                        }
                        is AlarmTransition.Applied -> db.alarmInstanceDao().upsert(t.instance.toEntity())
                        is AlarmTransition.Rejected -> Log.w(TAG, "snooze rejected: ${t.reason}")
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "snooze failed for alarm $alarmId", t)
                }
            }
            stopService(context)
        }

    /** Called by the ringing service when its own safety timeout elapses. */
    suspend fun expire(context: Context, alarmId: Long, triggerAt: Long) {
        silenceNow()
        NotificationHelper.cancelAlert(context, instanceIdFor(alarmId, triggerAt))
        val db = NexoraDatabase.get(context)
        val instanceId = instanceIdFor(alarmId, triggerAt)
        db.alarmInstanceDao().byId(instanceId)?.toDomain()?.let { inst ->
            when (val t = AlarmStateMachine.onExpired(inst)) {
                is AlarmTransition.Applied -> db.alarmInstanceDao().upsert(t.instance.toEntity())
                else -> Unit
            }
        }
        stopService(context)
    }

    /**
     * Brings persisted state back in line with reality after a crash or force-stop.
     * Any instance still marked Ringing when nothing is ringing becomes Expired, so the
     * UI never shows a phantom alarm and the next firing is treated as new.
     */
    suspend fun reconcileOnStartup(context: Context) = withContext(Dispatchers.IO) {
        runCatching {
            val db = NexoraDatabase.get(context)
            val ringing = db.alarmInstanceDao().ringing()
            val now = System.currentTimeMillis()
            ringing.forEach { entity ->
                val inst = entity.toDomain()
                when (val t = AlarmStateMachine.onExpired(inst)) {
                    is AlarmTransition.Applied -> db.alarmInstanceDao().upsert(t.instance.toEntity(now))
                    else -> Unit
                }
            }
            db.alarmInstanceDao().pruneFinished(now - 14L * 24 * 60 * 60 * 1000)
            db.alarmInstanceDao().pruneStaleRinging(now - 24 * 60 * 60 * 1000)
        }.onFailure { Log.e(TAG, "reconcile failed", it) }
    }

    suspend fun restoreAll(context: Context) = withContext(Dispatchers.IO) {
        runCatching {
            val repo = repo(context)
            val alarms = repo.alarms()
            AlarmScheduler(context).restoreAll(alarms)
            NotificationHelper.ensureChannel(context)
        }.onFailure { Log.e(TAG, "restoreAll failed", it) }
    }

    fun instanceIdFor(alarmId: Long, triggerAt: Long): String = "$alarmId@$triggerAt"

    private fun startService(context: Context, instance: AlarmInstance, alarm: Alarm) {
        val intent = AlarmRingingService.startIntent(context, instance.alarmId, instance.triggerAt)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "could not start ringing service", t)
        }
    }

    private fun stopService(context: Context) {
        runCatching { context.stopService(Intent(context, AlarmRingingService::class.java)) }
    }

    /** Late-binding access to the zone so tests can substitute it. */
    internal fun zone(): ZoneId = ZoneId.systemDefault()
}

/** Small ring buffer of recent alarm firings, used by the Alarm Health Centre. */
object AlarmHistory {
    private const val PREFS = "nexora_alarm_history"
    private const val KEY = "entries"

    fun record(triggerAt: Long, dismissedAt: Long, source: String) {
        // Persistence of history is deliberately in-memory-cheap: the DB instance rows
        // already carry firedLatencyMillis, which is what the health centre displays.
        lastDismissLatencyMillis = (dismissedAt - triggerAt).coerceAtLeast(0)
        lastDismissSource = source
    }

    @Volatile var lastDismissLatencyMillis: Long = 0L
        private set
    @Volatile var lastDismissSource: String = "none"
        private set
}
