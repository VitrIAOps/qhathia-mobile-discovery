package com.qhathia.mobile.discovery

import android.app.Application
import android.os.Build
import com.qhathia.mobile.discovery.core.domain.usecase.HandleDeviceUnlinkedUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ProcessDeviceCommandsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RunDeviceHeartbeatUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.SyncDeviceSettingsUseCase
import com.qhathia.mobile.discovery.di.ApplicationScope
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Application shell — Hilt's dependency graph root for the Discovery app. Starts the device
 * heartbeat (runs while the tablet is paired), the processor of the remote commands it delivers, and
 * the remote-settings sync it drives, and the handler that revokes the session when the backend reports
 * the tablet as unlinked — all for the process lifetime.
 *
 * Only the main process runs them. The short-lived `:restart` process ([RestartActivity]) also
 * creates this Application; the use cases are injected lazily so it builds none of them.
 */
@HiltAndroidApp
class DiscoveryApp : Application() {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var runDeviceHeartbeat: Lazy<RunDeviceHeartbeatUseCase>

    @Inject
    lateinit var processDeviceCommands: Lazy<ProcessDeviceCommandsUseCase>

    @Inject
    lateinit var syncDeviceSettings: Lazy<SyncDeviceSettingsUseCase>

    @Inject
    lateinit var handleDeviceUnlinked: Lazy<HandleDeviceUnlinkedUseCase>

    override fun onCreate() {
        super.onCreate()
        if (!isMainProcess()) return
        // Subscribed first, so a 401 DEVICE_UNLINKED from the very first heartbeat is not missed.
        applicationScope.launch { handleDeviceUnlinked.get()() }
        applicationScope.launch { runDeviceHeartbeat.get()() }
        applicationScope.launch { processDeviceCommands.get()() }
        applicationScope.launch { syncDeviceSettings.get()() }
    }

    private fun isMainProcess(): Boolean {
        val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getProcessName()
        } else {
            runCatching { File("/proc/self/cmdline").readText().trim('\u0000', ' ', '\n') }.getOrNull()
        }
        // Unknown name: behave as the main process (the previous behavior).
        return name == null || name == packageName
    }
}
