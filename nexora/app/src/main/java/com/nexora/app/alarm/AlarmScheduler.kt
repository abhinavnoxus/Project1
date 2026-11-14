package com.nexora.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.nexora.app.data.repo.AlarmSchedulerPort
import com.nexora.app.domain.alarm.AlarmMath
import com.nexora.app.domain.model.Alarm
import com.nexora.app.domain.model.AlarmKind
import java.time.ZoneId

/**
 * OS-level alarm scheduling.
 *
 * Design decisions that matter for reliability:
 *  - Every scheduled occurrence gets its own [PendingIntent] **and** its own request code
 *    derived from the alarm id and the occurrence minute, so an edit cannot leave an
 *    orphan occurrence that fires with old settings.
 *  - Exact alarms are used when the platform allows them; when `canScheduleExactAlarms`
 *    is false we fall back to inexact (`setAndAllowWhileIdle`) rather than dropping the
 *    alarm, and the Alarm Health Centre reports the degradation honestly.
 *  - Scheduling is idempotent: the same occurrence scheduled twice replaces itself
 *    because the PendingIntent is FLAG_UPDATE_CURRENT with the same identity.
 */
class AlarmScheduler(private val context: Context) : AlarmSchedulerPort {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override suspend fun reschedule(alarm: Alarm) {
        cancelAll(alarm.id)
        if (!alarm.enabled) return
        val now = System.currentTimeMillis()
        val trigger = AlarmMath.nextTrigger(alarm, now, ZoneId.systemDefault()) ?: return
        schedule(alarm, trigger)
    }

    fun schedule(alarm: Alarm, triggerAtMillis: Long) {
        val manager = alarmManager ?: return
        val pi = firingIntent(alarm, triggerAtMillis)
        val exactAllowed = canScheduleExact()

        runCatching {
            when {
                exactAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    manager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent(alarm)),
                        pi,
                    )
                exactAllowed ->
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
                else ->
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        }.onFailure {
            // Last-resort fallback: still better than silently losing the alarm.
            runCatching { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi) }
        }
    }

    override suspend fun cancelAll(alarmId: Long) {
        val manager = alarmManager ?: return
        // We cannot enumerate PendingIntents, so we cancel every occurrence we might have
        // created: the next 8 days at every minute-of-day the alarm could hold.
        val alarm = knownOccurrences(alarmId) ?: return
        alarm.forEach { manager.cancel(it) }
    }

    /** PendingIntents currently registered for this alarm id (tracked in prefs). */
    private fun knownOccurrences(alarmId: Long): List<PendingIntent>? {
        val store = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val keys = store.getStringSet(key(alarmId), null) ?: return null
        return keys.mapNotNull { encoded ->
            val parts = encoded.split(':')
            if (parts.size != 2) return@mapNotNull null
            val trigger = parts[0].toLongOrNull() ?: return@mapNotNull null
            val requestCode = parts[1].toIntOrNull() ?: return@mapNotNull null
            PendingIntent.getBroadcast(
                context, requestCode,
                AlarmReceiver.intentFor(context, alarmId, trigger),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }

    private fun trackOccurrence(alarmId: Long, triggerAt: Long, requestCode: Int) {
        val store = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = store.getStringSet(key(alarmId), emptySet()).orEmpty().toMutableSet()
        set += "$triggerAt:$requestCode"
        store.edit().putStringSet(key(alarmId), set).apply()
    }

    fun canScheduleExact(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager?.canScheduleExactAlarms() == true
    } else {
        true
    }

    fun nextAlarmMillis(): Long? = alarmManager?.nextAlarmClock?.triggerTime

    private fun firingIntent(alarm: Alarm, triggerAt: Long): PendingIntent {
        val requestCode = AlarmMath.requestCode(alarm.id, triggerAt)
        trackOccurrence(alarm.id, triggerAt, requestCode)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            AlarmReceiver.intentFor(context, alarm.id, triggerAt),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Tapping the system alarm icon opens NEXORA's alarm screen for this alarm. */
    private fun showIntent(alarm: Alarm): PendingIntent = PendingIntent.getActivity(
        context,
        AlarmMath.requestCode(alarm.id, 0L),
        Intent(context, com.nexora.app.MainActivity::class.java).apply {
            action = com.nexora.app.MainActivity.ACTION_SHOW_ALARM
            putExtra(com.nexora.app.MainActivity.EXTRA_ALARM_ID, alarm.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Re-arms everything after boot, package replace or a time-zone change. */
    suspend fun restoreAll(alarms: List<Alarm>) {
        alarms.forEach { alarm ->
            when {
                !alarm.enabled -> Unit
                alarm.kind == AlarmKind.OneShot && (alarm.oneShotAt ?: 0L) <= System.currentTimeMillis() -> Unit
                else -> reschedule(alarm)
            }
        }
    }

    fun ringtoneAvailable(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return true
        val uri = Uri.parse(uriString)
        return runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
    }

    companion object {
        private const val PREFS = "nexora_alarm_occurrences"
        private fun key(alarmId: Long) = "alarm_$alarmId"
    }
}
