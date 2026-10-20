package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.settings.DeviceSettingsRepositoryImpl
import com.qhathia.mobile.discovery.core.data.settings.SettingsAccessRepositoryImpl
import com.qhathia.mobile.discovery.core.domain.repository.DeviceSettingsRepository
import com.qhathia.mobile.discovery.core.domain.repository.SettingsAccessRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the settings-access repository (exit PIN validation) and the remote device settings repository. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsDataModule {

    @Binds
    @Singleton
    abstract fun bindSettingsAccessRepository(
        impl: SettingsAccessRepositoryImpl,
    ): SettingsAccessRepository

    @Binds
    @Singleton
    abstract fun bindDeviceSettingsRepository(
        impl: DeviceSettingsRepositoryImpl,
    ): DeviceSettingsRepository
}
