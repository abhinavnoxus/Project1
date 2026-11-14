package com.nexora.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.nexora.app.notify.ReminderReceiver
import java.time.ZoneId

/**
 * Schedules task / routine reminders.
 *
 * Deliberately separate from the alarm engine: reminders are inexact, non-ringing
 * notifications and must never hold the alarm audio path hostage. Using
 * `setAndAllowWhileIdle` (not `setExactAndAllowWhileIdle`) avoids burning the device's
 * scarce exact-alarm budget on non-critical nudges.
 */
object ReminderScheduler {

    fun schedule(context: Context, taskId: Long, title: String, body: String, atMillis: Long) {
        if (atMillis <= System.currentTimeMillis()) return
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pendingIntent(context, taskId, title, body)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            } else {
                manager.set(AlarmManager.RTC_WAKEUP, atMillis, pi)
            }
        }.onFailure { Log.e("NexoraReminder", "schedule failed for task $taskId", it) }
    }

    fun cancel(context: Context, taskId: Long) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        }
        val pi = PendingIntent.getBroadcast(
            context, requestCode(taskId), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        manager.cancel(pi)
    }

    /** Schedules every reminder that is due in the next [horizonHours]. Idempotent. */
    fun rescheduleAll(context: Context, due: List<Triple<Long, String, Long>>) {
        val now = System.currentTimeMillis()
        due.filter { it.third > now }.forEach { (id, title, at) ->
            schedule(context, id, title, "NEXORA reminder", at)
        }
    }

    private fun pendingIntent(context: Context, taskId: Long, title: String, body: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_BODY, body)
        }
        return PendingIntent.getBroadcast(
            context, requestCode(taskId), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCode(taskId: Long): Int = (taskId % 100_000L).toInt() + 10_000

    fun zone(): ZoneId = ZoneId.systemDefault()
}
