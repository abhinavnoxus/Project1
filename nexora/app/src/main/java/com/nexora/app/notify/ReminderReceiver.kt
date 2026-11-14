package com.nexora.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nexora.app.notify.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Delivers a non-ringing reminder notification for a task. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "A NEXORA reminder is due."

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationHelper.ensureChannel(context)
                NotificationHelper.postReminder(context, taskId, title, body)
            } catch (t: Throwable) {
                Log.e("NexoraReminder", "deliver failed for task $taskId", t)
            } finally {
                runCatching { pending.finish() }
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.nexora.app.reminder.DELIVER"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
    }
}
