package com.nexora.app.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.nexora.app.MainActivity
import com.nexora.app.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Home-screen widget: live clock, date and next alarm.
 *
 * It never runs a background service. Instead it (a) refreshes on every system
 * `APPWIDGET_UPDATE` broadcast, and (b) schedules a single inexact wake-up for the next
 * minute boundary only while a widget exists. When the last widget is removed, the
 * wake-up is cancelled.
 */
class NexoraWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> manager.updateAppWidget(id, build(context)) }
        scheduleTick(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TICK || intent.action == Intent.ACTION_TIME_TICK) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NexoraWidgetProvider::class.java))
            ids.forEach { id -> manager.updateAppWidget(id, build(context)) }
            if (ids.isNotEmpty()) scheduleTick(context)
            return
        }
        super.onReceive(context, intent)
    }

    override fun onDisabled(context: Context) {
        cancelTick(context)
        super.onDisabled(context)
    }

    private fun build(context: Context): RemoteViews {
        val now = LocalDateTime.now()
        val views = RemoteViews(context.packageName, R.layout.widget_nexora)
        views.setTextViewText(R.id.widget_time, now.format(DateTimeFormatter.ofPattern("HH:mm")))
        views.setTextViewText(R.id.widget_date, now.format(DateTimeFormatter.ofPattern("EEE d MMM")))
        val next = (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.nextAlarmClock?.triggerTime
        views.setTextViewText(
            R.id.widget_next,
            next?.let { ms ->
                val at = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
                "Next alarm ${at.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            } ?: "No alarm set",
        )
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_ALARMS
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_time, open)
        views.setOnClickPendingIntent(R.id.widget_date, open)
        views.setOnClickPendingIntent(R.id.widget_next, open)
        return views
    }

    private fun scheduleTick(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NexoraWidgetProvider::class.java).apply { action = ACTION_TICK }
        val pi = PendingIntent.getBroadcast(
            context, 9001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val nextMinute = (System.currentTimeMillis() / 60_000L + 1) * 60_000L
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC, nextMinute, pi)
            } else {
                manager.set(AlarmManager.RTC, nextMinute, pi)
            }
        }
    }

    private fun cancelTick(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NexoraWidgetProvider::class.java).apply { action = ACTION_TICK }
        val pi = PendingIntent.getBroadcast(
            context, 9001, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        manager.cancel(pi)
    }

    private companion object {
        const val ACTION_TICK = "com.nexora.app.WIDGET_TICK"
    }
}
