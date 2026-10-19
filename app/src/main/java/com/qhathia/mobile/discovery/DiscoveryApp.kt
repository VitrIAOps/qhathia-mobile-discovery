package com.qhathia.mobile.discovery

import android.app.Application
import com.qhathia.mobile.discovery.core.domain.usecase.ProcessDeviceCommandsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RunDeviceHeartbeatUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.SyncDeviceSettingsUseCase
import com.qhathia.mobile.discovery.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application shell — Hilt's dependency graph root for the Discovery app. Starts the device
 * heartbeat (runs while the tablet is paired), the processor of the remote commands it delivers, and
 * the remote-settings sync it drives — all for the process lifetime.
 */
@HiltAndroidApp
class DiscoveryApp : Application() {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var runDeviceHeartbeat: RunDeviceHeartbeatUseCase

    @Inject
    lateinit var processDeviceCommands: ProcessDeviceCommandsUseCase

    @Inject
    lateinit var syncDeviceSettings: SyncDeviceSettingsUseCase

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { runDeviceHeartbeat() }
        applicationScope.launch { processDeviceCommands() }
        applicationScope.launch { syncDeviceSettings() }
    }
}
