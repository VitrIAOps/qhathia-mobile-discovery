package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.settings.SettingsAccessRepositoryImpl
import com.qhathia.mobile.discovery.core.domain.repository.SettingsAccessRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the settings-access repository (mocked password validation) into the singleton graph. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsDataModule {

    @Binds
    @Singleton
    abstract fun bindSettingsAccessRepository(
        impl: SettingsAccessRepositoryImpl,
    ): SettingsAccessRepository
}
