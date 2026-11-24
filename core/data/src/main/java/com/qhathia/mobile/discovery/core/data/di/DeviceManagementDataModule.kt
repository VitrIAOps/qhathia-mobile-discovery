package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.management.AppDeviceRestarter
import com.qhathia.mobile.discovery.core.data.management.BackendDeviceCommandSource
import com.qhathia.mobile.discovery.core.data.management.BackendRemoteSettingsSource
import com.qhathia.mobile.discovery.core.data.management.DiscoveryDeviceManagementConfig
import com.qhathia.mobile.discovery.core.data.management.HeartbeatDeviceReporter
import com.qhathia.mobile.discovery.core.data.management.PendingRestartAckStore
import com.qhathia.mobile.discovery.core.data.management.discoveryCommandProcessor
import com.qhathia.mobile.discovery.core.device.management.HeartbeatRunner
import com.qhathia.mobile.discovery.core.device.management.RemoteCommandProcessor
import com.qhathia.mobile.discovery.core.device.management.RemoteSettingsSync
import com.qhathia.mobile.discovery.core.domain.repository.CatalogRefreshSignal
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/** Builds the `core:device` management engines over Discovery's repositories. */
@Module
@InstallIn(SingletonComponent::class)
internal object DeviceManagementDataModule {

    @Provides
    fun provideHeartbeatRunner(reporter: HeartbeatDeviceReporter): HeartbeatRunner =
        HeartbeatRunner(reporter, DiscoveryDeviceManagementConfig)

    @Provides
    fun provideRemoteCommandProcessor(
        source: BackendDeviceCommandSource,
        catalogRefreshSignal: CatalogRefreshSignal,
        restarter: AppDeviceRestarter,
        pendingRestart: PendingRestartAckStore,
        clock: Clock,
    ): RemoteCommandProcessor =
        discoveryCommandProcessor(source, catalogRefreshSignal, restarter, pendingRestart, clock)

    @Provides
    fun provideRemoteSettingsSync(source: BackendRemoteSettingsSource): RemoteSettingsSync =
        RemoteSettingsSync(source)
}
