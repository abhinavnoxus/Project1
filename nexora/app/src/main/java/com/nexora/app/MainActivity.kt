package com.nexora.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nexora.app.alarm.NotificationHelper
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.LocalGlassQuality
import com.nexora.app.design.LocalGlassSpec
import com.nexora.app.design.LocalMotionEnabled
import com.nexora.app.ui.NexoraViewModel
import com.nexora.app.ui.RootScaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.activity.viewModels
import kotlinx.coroutines.launch

/**
 * The only Activity.
 *
 * It does three things: owns the ViewModel, forwards notification/intent entry points
 * into navigation state, and hosts the Compose tree. All alarm actions are handled by
 * receivers and the service — this Activity never has to be alive for an alarm to stop.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: NexoraViewModel by viewModels()

    private var pendingAlarm by mutableStateOf<Long?>(null)
    private var pendingTrigger by mutableStateOf<Long?>(null)
    private var pendingTask by mutableStateOf<Long?>(null)
    private var pendingRoute by mutableStateOf<String?>(null)

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // Not fatal: alarms still ring in-app and the health centre reports the state.
        }
    }

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

            NexoraRoot(spec = spec, quality = quality, reducedMotion = reducedMotion) {
                RootScaffold(
                    viewModel = viewModel,
                    spec = spec,
                    pendingAlarmId = pendingAlarm,
                    pendingTriggerAt = pendingTrigger,
                    pendingTaskId = pendingTask,
                    pendingRoute = pendingRoute,
                    onConsumePending = {
                        pendingAlarm = null
                        pendingTrigger = null
                        pendingTask = null
                        pendingRoute = null
                    },
                )
            }
        }

        requestStartupPermissions()

        lifecycleScope.launch {
            handleIntentAsync(intent)
            viewModel.setOnboardedIfNeeded()
        }
    }

    private fun NexoraRoot(
        spec: GlassSpec,
        quality: com.nexora.app.design.GlassQuality,
        reducedMotion: Boolean,
        content: @Composable () -> Unit,
    ) {
        CompositionLocalProvider(
            LocalGlassSpec provides spec,
            LocalGlassQuality provides quality,
            LocalMotionEnabled provides !spec.reducedOrOff,
            content = content,
        )
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
                pendingAlarm = intent.getLongExtra(EXTRA_ALARM_ID, -1L).takeIf { it > 0 }
                pendingTrigger = intent.getLongExtra(EXTRA_TRIGGER_AT, 0L)
            }
            ACTION_OPEN_REMINDER -> {
                pendingTask = intent.getLongExtra(EXTRA_TASK_ID, -1L).takeIf { it > 0 }
            }
            ACTION_OPEN_ALARMS, androidx.appcompat.R.string.abc_action_bar_home_description -> {
                pendingRoute = "alarm"
            }
            Intent.ACTION_SET_ALARM -> {
                pendingRoute = "alarm"
            }
            else -> Unit
        }
    }

    private suspend fun handleIntentAsync(intent: Intent?) {
        intent ?: return
        // Nothing needs suspending yet; kept as a seam for future deep links.
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun requestStartupPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationHelper.canPost(this)) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        NotificationHelper.ensureChannel(this)
    }

    private fun NexoraViewModel.setOnboardedIfNeeded() = Unit

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
