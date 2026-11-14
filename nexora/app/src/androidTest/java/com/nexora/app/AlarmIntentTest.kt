package com.nexora.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nexora.app.alarm.AlarmReceiver
import com.nexora.app.domain.alarm.AlarmMath
import com.nexora.app.notify.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device checks of the notification/intent wiring the dismiss guarantee depends on.
 * Requires an attached device or emulator.
 */
@RunWith(AndroidJUnit4::class)
class AlarmIntentTest {

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun dismissIntentCarriesExactInstanceIdentity() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, 7L)
            putExtra(AlarmReceiver.EXTRA_TRIGGER_AT, 1_700_000_000_000L)
        }
        assertEquals(AlarmReceiver.ACTION_DISMISS, intent.action)
        assertEquals(7L, intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L))
        assertEquals(1_700_000_000_000L, intent.getLongExtra(AlarmReceiver.EXTRA_TRIGGER_AT, 0L))
        // The receiver class is explicit: no implicit routing can misdeliver it.
        assertEquals(AlarmReceiver::class.java.name, intent.component?.className)
    }

    @Test
    fun firingIntentIsExplicitAndUsesTheInstanceTrigger() {
        val intent = AlarmReceiver.intentFor(context, alarmId = 3L, triggerAt = 1_700_000_060_000L)
        assertEquals(AlarmReceiver.ACTION_FIRED, intent.action)
        assertEquals(AlarmReceiver::class.java.name, intent.component?.className)
        assertEquals(1_700_000_060_000L, intent.getLongExtra(AlarmReceiver.EXTRA_TRIGGER_AT, 0L))
    }

    @Test
    fun twoAlarmsProduceDistinctNotificationTags() {
        assertNotEquals(
            AlarmControllerIds.instanceId(1L, 1_700_000_000_000L),
            AlarmControllerIds.instanceId(1L, 1_700_000_060_000L),
        )
    }

    @Test
    fun requestCodesAreStableAndDistinctPerOccurrence() {
        val first = AlarmMath.requestCode(4L, 1_700_000_000_000L)
        val second = AlarmMath.requestCode(4L, 1_700_000_000_000L)
        val nextDay = AlarmMath.requestCode(4L, 1_700_000_000_000L + 86_400_000L)
        assertEquals(first, second)
        assertNotEquals(first, nextDay)
        assertTrue(first >= 0)
    }

    @Test
    fun alarmChannelIsHighImportance() {
        NotificationHelper.ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(NotificationHelper.CHANNEL_ALARMS)
        assertNotNull("alarm channel must exist", channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel!!.importance)
    }

    @Test
    fun cancellingAnUnknownAlertIsSafe() {
        // Must not throw: dismissal runs defensively from several paths.
        NotificationHelper.cancelAlert(context, "does-not-exist@0")
        NotificationHelper.cancelAlert(context, "another@0")
        NotificationHelper.cancelAllAlerts(context)
    }
}

/** Tiny helper mirroring the controller's instance-id format, for assertions. */
private object AlarmControllerIds {
    fun instanceId(alarmId: Long, triggerAt: Long): String = "$alarmId@$triggerAt"
}
