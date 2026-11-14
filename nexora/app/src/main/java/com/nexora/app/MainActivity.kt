package com.nexora.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexora.app.alarm.NotificationHelper
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.LocalGlassQuality
import com.nexora.app.design.LocalGlassSpec
import com.nexora.app.design.LocalMotionEnabled
import com.nexora.app.ui.NexoraViewModel
import com.nexora.app.ui.RootScaffold

/**
 * The only Activity.
 *
 * Responsibilities are deliberately narrow: own the ViewModel, translate incoming
 * intents (notification taps, SHOW_ALARMS, SET_ALARM) into navigation state, and host the
 * Compose tree. **No alarm action depends on this Activity being alive** — stopping an
 * alarm is handled by a BroadcastReceiver and the foreground service, so a dead or
 * destroyed Activity cannot leave a ringtone playing.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: NexoraViewModel by viewModels()

    private var pendingAlarmId by mutableStateOf<Long?>(null)
    private var pendingTriggerAt by mutableStateOf<Long?>(null)
    private var pendingTaskId by mutableStateOf<Long?>(null)
    private var pendingRoute by mutableStateOf<String?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* reported in Health Centre */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        handleIntent(intent)

        setContent {
            val profileSet by viewModel.profiles.collectAsStateWithLifecycle()
            val quality by viewModel.effectiveQuality.collectAsStateWithLifecycle()
            val reducedMotion by viewModel.reducedMotion.collectAsStateWithLifecycle()
            val spec: GlassSpec = profileSet.spec(reducedMotion)

            CompositionLocalProvider(
                LocalGlassSpec provides spec,
                LocalGlassQuality provides quality,
                LocalMotionEnabled provides !spec.reducedOrOff,
            ) {
                RootScaffold(
                    viewModel = viewModel,
                    spec = spec,
                    pendingAlarmId = pendingAlarmId,
                    pendingTriggerAt = pendingTriggerAt,
                    pendingTaskId = pendingTaskId,
                    pendingRoute = pendingRoute,
                    onConsumePending = {
                        pendingAlarmId = null
                        pendingTriggerAt = null
                        pendingTaskId = null
                        pendingRoute = null
                    },
                )
            }
        }

        requestStartupPermissions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            ACTION_ALARM_RINGING -> {
                pendingAlarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L).takeIf { it > 0L }
                pendingTriggerAt = intent.getLongExtra(EXTRA_TRIGGER_AT, 0L).takeIf { it > 0L }
            }
            ACTION_OPEN_REMINDER ->
                pendingTaskId = intent.getLongExtra(EXTRA_TASK_ID, -1L).takeIf { it > 0L }
            ACTION_OPEN_ALARMS, ACTION_SHOW_ALARM, android.provider.Settings.ACTION_SET_ALARM, "android.intent.action.SHOW_ALARMS" ->
                pendingRoute = "alarms"
            else -> Unit
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun requestStartupPermissions() {
        NotificationHelper.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.canPost(this)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val ACTION_ALARM_RINGING = "com.nexora.app.ALARM_RINGING"
        const val ACTION_OPEN_ALARMS = "com.nexora.app.OPEN_ALARMS"
        const val ACTION_OPEN_REMINDER = "com.nexora.app.OPEN_REMINDER"
        const val ACTION_SHOW_ALARM = "com.nexora.app.SHOW_ALARM"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_TRIGGER_AT = "trigger_at"
        const val EXTRA_TASK_ID = "task_id"
    }
}
