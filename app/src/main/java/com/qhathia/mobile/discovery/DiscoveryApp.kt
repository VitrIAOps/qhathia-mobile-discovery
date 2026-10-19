package com.qhathia.mobile.discovery

import android.app.Application
import com.qhathia.mobile.discovery.core.domain.usecase.RunDeviceHeartbeatUseCase
import com.qhathia.mobile.discovery.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application shell — Hilt's dependency graph root for the Discovery app. Starts the device
 * heartbeat, which runs for the process lifetime while the tablet is paired.
 */
@HiltAndroidApp
class DiscoveryApp : Application() {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var runDeviceHeartbeat: RunDeviceHeartbeatUseCase

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { runDeviceHeartbeat() }
    }
}
