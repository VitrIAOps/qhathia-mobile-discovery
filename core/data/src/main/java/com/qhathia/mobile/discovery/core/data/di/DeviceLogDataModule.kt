package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.devicelog.DeviceLogRepositoryImpl
import com.qhathia.mobile.discovery.core.domain.repository.DeviceLogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the device log repository (`POST /api/v1/devices/logs`). */
@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceLogDataModule {

    @Binds
    @Singleton
    abstract fun bindDeviceLogRepository(impl: DeviceLogRepositoryImpl): DeviceLogRepository
}
