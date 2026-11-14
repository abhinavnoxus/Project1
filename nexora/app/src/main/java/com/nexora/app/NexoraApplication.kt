package com.nexora.app

import android.app.Application
import android.os.Build
import com.nexora.app.alarm.AlarmController
import com.nexora.app.notify.NotificationHelper
import com.nexora.app.data.NexoraDatabase
import com.nexora.app.notify.NotificationHelper
import com.nexora.app.data.repo.FocusRepository
import com.nexora.app.platform.DeviceProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Process entry point.
 *
 * Startup work is deliberately minimal and off the main thread: channels, the device
 * profile, orphaned-session cleanup and alarm reconciliation all happen on an
 * application-scoped IO dispatcher. Nothing here blocks the first frame.
 */
class NexoraApplication : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var deviceProfile: DeviceProfile
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        deviceProfile = DeviceProfile.detect(this)

        appScope.launch {
            runCatching { NotificationHelper.ensureChannel(this@NexoraApplication) }
            runCatching { AlarmController.reconcileOnStartup(this@NexoraApplication) }
            runCatching { FocusRepository(NexoraDatabase.get(this@NexoraApplication)).closeOrphans() }
        }
    }

    companion object {
        @Volatile private var instance: NexoraApplication? = null

        fun get(): NexoraApplication? = instance

        val sdkInt: Int get() = Build.VERSION.SDK_INT
    }
}
