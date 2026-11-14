package com.nexora.app.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.nexora.app.data.NexoraDatabase
import com.nexora.app.data.repo.toDomain
import com.nexora.app.domain.alarm.AlarmMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the ring.
 *
 * It is started with `startForegroundService` from the receiver and promotes itself to
 * the foreground in `onCreate` (before any suspension point) so Android 12+ cannot kill
 * it for missing the 5-second window. The service holds no permanent wakelock: the
 * MediaPlayer and periodic vibration are enough to keep the device awake, and a hard
 * timeout guarantees it stops even if nobody ever taps the notification.
 */
class AlarmRingingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ringer: AlarmRinger? = null
    private var timeoutJob: Job? = null
    private var currentAlarmId: Long = -1L
    private var currentTriggerAt: Long = 0L

    override fun onCreate() {
        super.onCreate()
        // Promote immediately; notification content is refined once the DB read returns.
        val placeholder = NotificationHelper.buildAlertNotification(
            context = this,
            instanceId = "boot",
            alarmId = -1L,
            triggerAt = 0L,
            label = "Alarm",
            snoozeMinutes = 10,
            canSnooze = true,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.ALERT_NOTIFICATION_ID,
                placeholder,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NotificationHelper.ALERT_NOTIFICATION_ID, placeholder)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
        val triggerAt = intent?.getLongExtra(AlarmReceiver.EXTRA_TRIGGER_AT, 0L) ?: 0L
        val phase = intent?.getStringExtra(EXTRA_PHASE)

        if (phase == PHASE_STOP || alarmId <= 0L) {
            stopSelfSafely()
            return START_NOT_STICKY
        }

        // Ignore a duplicate start for the alarm already ringing.
        if (alarmId == currentAlarmId && triggerAt == currentTriggerAt && ringer != null) {
            return START_NOT_STICKY
        }
        currentAlarmId = alarmId
        currentTriggerAt = triggerAt

        scope.launch {
            try {
                val db = NexoraDatabase.get(applicationContext)
                val alarm = db.alarmDao().getById(alarmId)?.toDomain()
                val label = alarm?.label ?: "Alarm"
                val snoozeMinutes = alarm?.snoozeMinutes ?: 10
                val canSnooze = (alarm?.maxSnoozes ?: 3) > 0

                val instanceId = AlarmController.instanceIdFor(alarmId, triggerAt)
                val notification = NotificationHelper.buildAlertNotification(
                    context = this@AlarmRingingService,
                    instanceId = instanceId,
                    alarmId = alarmId,
                    triggerAt = triggerAt,
                    label = label,
                    snoozeMinutes = snoozeMinutes,
                    canSnooze = canSnooze,
                )
                NotificationHelper.postAlert(this@AlarmRingingService, instanceId, notification)

                // Audio starts only after the notification exists: if the process is
                // killed between the two, the user still sees a dismissable alarm.
                val r = AlarmRinger(this@AlarmRingingService)
                ringer = r
                AlarmController.attachRinger(r)
                r.start(alarm?.ringtoneUri, alarm?.vibrate ?: true, alarm?.fadeInSeconds ?: 8)

                timeoutJob?.cancel()
                timeoutJob = scope.launch {
                    delay(AlarmMath.ringTimeoutMillis(alarm ?: com.nexora.app.domain.model.Alarm(id = alarmId)))
                    Log.i(TAG, "ring timeout for alarm $alarmId")
                    AlarmController.expire(applicationContext, alarmId, triggerAt)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "ringing setup failed", t)
                AlarmController.expire(applicationContext, alarmId, triggerAt)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        timeoutJob?.cancel()
        ringer?.stop()
        if (AlarmController.let { true }) {
            // Detach only if we are still the attached ringer.
            AlarmController.attachRinger(null)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopSelfSafely() {
        ringer?.stop()
        ringer = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION") stopForeground(true)
        }
        stopSelf()
    }

    companion object {
        private const val TAG = "NexoraRingService"
        const val EXTRA_PHASE = "phase"
        const val PHASE_STOP = "stop"

        fun startIntent(context: Context, alarmId: Long, triggerAt: Long): Intent =
            Intent(context, AlarmRingingService::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmReceiver.EXTRA_TRIGGER_AT, triggerAt)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, AlarmRingingService::class.java).apply {
                putExtra(EXTRA_PHASE, PHASE_STOP)
            }
    }
}
