package com.nexora.app.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nexora.app.MainActivity
import com.nexora.app.alarm.AlarmReceiver
import com.nexora.app.R

/**
 * Notification plumbing for alarms and reminders.
 *
 * Key correctness points:
 *  - The alert notification's identity is the **instance id**, so two alarms ringing at
 *    once get two notifications and cancelling one never cancels the other.
 *  - All PendingIntents are explicit, immutable and carry the instance identity, so a
 *    stale notification cannot dismiss a newer firing.
 *  - The dismiss/snooze actions are broadcast (not activity) so they work with the screen
 *    locked and the activity destroyed.
 */
object NotificationHelper {

    const val CHANNEL_ALARMS = "nexora_alarms_v1"
    const val CHANNEL_REMINDERS = "nexora_reminders_v1"
    const val CHANNEL_STATUS = "nexora_status_v1"
    const val ALERT_NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val alarms = NotificationChannel(
            CHANNEL_ALARMS,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alarm rings. Cannot be silenced by the system until dismissed."
            enableVibration(true)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null) // NEXORA plays its own ringtone for fade-in control.
            enableLights(true)
        }

        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            "Reminders & tasks",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Task reminders, routine nudges and focus-session alerts."
        }

        val status = NotificationChannel(
            CHANNEL_STATUS,
            "Status",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing status such as an active focus session."
            setShowBadge(false)
        }

        manager.createNotificationChannels(listOf(alarms, reminders, status))
    }

    fun buildAlertNotification(
        context: Context,
        instanceId: String,
        alarmId: Long,
        triggerAt: Long,
        label: String,
        snoozeMinutes: Int,
        canSnooze: Boolean,
    ): Notification {
        val dismissIntent = PendingIntent.getBroadcast(
            context,
            requestCode(alarmId, triggerAt, 11),
            broadcast(context, AlarmReceiver.ACTION_DISMISS, alarmId, triggerAt),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            requestCode(alarmId, triggerAt, 12),
            broadcast(context, AlarmReceiver.ACTION_SNOOZE, alarmId, triggerAt),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openIntent = PendingIntent.getActivity(
            context,
            requestCode(alarmId, triggerAt, 13),
            Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_ALARM_RINGING
                putExtra(MainActivity.EXTRA_ALARM_ID, alarmId)
                putExtra(MainActivity.EXTRA_TRIGGER_AT, triggerAt)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_nexora_alarm)
            .setContentTitle(label.ifBlank { "Alarm" })
            .setContentText(if (canSnooze) "Tap dismiss, or snooze for $snoozeMinutes min" else "Tap dismiss")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(openIntent)
            .setFullScreenIntent(openIntent, true)
            .setDeleteIntent(dismissIntent)
            .addAction(R.drawable.ic_nexora_stop, "Dismiss", dismissIntent)

        if (canSnooze) {
            builder.addAction(R.drawable.ic_nexora_snooze, "Snooze $snoozeMinutes min", snoozeIntent)
        }
        return builder.build()
    }

    fun postAlert(context: Context, instanceId: String, notification: Notification) {
        if (!canPost(context)) return
        runCatching {
            NotificationManagerCompat.from(context).notify(instanceId, ALERT_NOTIFICATION_ID, notification)
        }
    }

    /** Cancels one instance's alert notification. Idempotent. */
    fun cancelAlert(context: Context, instanceId: String) {
        runCatching { NotificationManagerCompat.from(context).cancel(instanceId, ALERT_NOTIFICATION_ID) }
    }

    fun cancelAllAlerts(context: Context) {
        runCatching {
            val manager = NotificationManagerCompat.from(context)
            manager.activeNotifications
                .filter { it.id == ALERT_NOTIFICATION_ID }
                .forEach { manager.cancel(it.tag, it.id) }
        }
    }

    fun postReminder(context: Context, taskId: Long, title: String, body: String) {
        if (!canPost(context)) return
        val openIntent = PendingIntent.getActivity(
            context,
            requestCode(taskId, taskId, 21),
            Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_REMINDER
                putExtra(MainActivity.EXTRA_TASK_ID, taskId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_nexora_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(TAG_REMINDER + taskId, REMINDER_ID_BASE + (taskId % 1000).toInt(), notification) }
    }

    fun canPost(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun areChannelsEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        val channel = manager.getNotificationChannel(CHANNEL_ALARMS) ?: return false
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun broadcast(context: Context, action: String, alarmId: Long, triggerAt: Long): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_TRIGGER_AT, triggerAt)
        }

    private fun requestCode(alarmId: Long, triggerAt: Long, salt: Int): Int =
        ((alarmId * 131 + triggerAt / 60_000L) * 31 + salt).toInt() and 0x7FFFFFFF

    private const val TAG_REMINDER = "nexora_reminder_"
    private const val REMINDER_ID_BASE = 2000
}
