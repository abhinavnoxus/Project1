package com.nexora.app.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * OEM battery-restriction guidance.
 *
 * Realme UI (and ColorOS under it) aggressively freezes background apps. We cannot change
 * that from code, so instead of pretending, NEXORA surfaces the exact settings screens the
 * user needs and explains why. Every launch is a plain, read-only intent with a
 * `runCatching` guard for OEMs that removed the activity.
 */
object PowerGuidance {

    fun isIgnoringBatteryOptimisations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Opens the system dialog asking to exempt NEXORA from battery optimisation. */
    fun requestIgnoreBatteryOptimisation(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { openAppDetails(context) }
    }

    fun openAppDetails(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure { openAppDetails(context) }
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure { openAppDetails(context) }
    }

    /** Human-readable notes specific to the OEM, shown in the Alarm Health Centre. */
    fun oemNotes(profile: DeviceProfile): List<String> = buildList {
        if (profile.isRealmeUi) {
            add("Realme UI: allow NEXORA to \"Auto-start\" and set Battery → App battery management to \"Allow background activity\".")
            add("Realme UI: lock NEXORA in the recent-apps view so the system does not clear it.")
            add("Realme UI: in Settings → Notification → NEXORA, enable \"Lock screen\", \"Banner\" and \"Sound\" for the Alarms channel.")
        }
        if (!profile.supportsBlur) {
            add("This device runs Android ${profile.androidRelease}, which has no GPU layer blur. NEXORA uses its cached-gradient glass instead — visually equivalent at a fraction of the cost.")
        }
    }
}
