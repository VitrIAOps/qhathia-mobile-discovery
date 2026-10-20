package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.heartbeat.DeviceHeartbeatRepositoryImpl
import com.qhathia.mobile.discovery.core.data.heartbeat.DeviceTelemetryProviderImpl
import com.qhathia.mobile.discovery.core.domain.repository.DeviceHeartbeatRepository
import com.qhathia.mobile.discovery.core.domain.repository.DeviceTelemetryProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the device heartbeat repository and the platform telemetry provider. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceHeartbeatDataModule {

    @Binds
    @Singleton
    abstract fun bindDeviceHeartbeatRepository(impl: DeviceHeartbeatRepositoryImpl): DeviceHeartbeatRepository

    @Binds
    @Singleton
    abstract fun bindDeviceTelemetryProvider(impl: DeviceTelemetryProviderImpl): DeviceTelemetryProvider
}
