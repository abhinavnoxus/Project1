package com.nexora.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires an alarm. Runs in a broadcast receiver, so it must (a) start the foreground
 * service immediately via `startForegroundService`, and (b) hold a short wake lock so the
 * service actually gets to start before the process is frozen.
 *
 * Every intent here is explicit and immutable, and carries the alarm id plus the exact
 * trigger instant — never a bare "alarm id" that could resolve to a newer occurrence.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val triggerAt = intent.getLongExtra(EXTRA_TRIGGER_AT, 0L)
        if (alarmId <= 0L) return

        val action = intent.action ?: ACTION_FIRED
        Log.i(TAG, "onReceive action=$action alarm=$alarmId trigger=$triggerAt")

        val wake = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nexora:alarm-dispatch")
            ?.apply { acquire(15_000L) }

        val pending = goAsync()
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                when (action) {
                    ACTION_SNOOZE -> AlarmController.snooze(context, alarmId, triggerAt)
                    ACTION_DISMISS -> AlarmController.dismiss(context, alarmId, triggerAt, source = "receiver")
                    else -> AlarmController.fire(context, alarmId, triggerAt)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "alarm dispatch failed", t)
            } finally {
                runCatching { if (wake?.isHeld == true) wake.release() }
                runCatching { pending.finish() }
            }
        }
    }

    companion object {
        private const val TAG = "NexoraAlarm"
        const val ACTION_FIRED = "com.nexora.app.alarm.FIRED"
        const val ACTION_DISMISS = "com.nexora.app.alarm.DISMISS"
        const val ACTION_SNOOZE = "com.nexora.app.alarm.SNOOZE"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_TRIGGER_AT = "trigger_at"

        fun intentFor(context: Context, alarmId: Long, triggerAt: Long): Intent =
            Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_FIRED
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_TRIGGER_AT, triggerAt)
            }
    }
}

/** Re-arms alarms after the device restarts. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val relevant = intent.action in setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )
        if (!relevant) return
        val pending = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                AlarmController.restoreAll(context)
                NotificationHelper.ensureChannel(context)
            } catch (t: Throwable) {
                Log.e("NexoraAlarm", "restore after ${intent.action} failed", t)
            } finally {
                runCatching { pending.finish() }
            }
        }
    }
}

/** Android 13+ requires the notification permission; we react by telling the user. */
class PermissionStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    AlarmController.restoreAll(context)
                } finally {
                    runCatching { pending.finish() }
                }
            }
        }
    }
}
