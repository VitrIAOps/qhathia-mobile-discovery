package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.kiosk.KioskSessionRepositoryImpl
import com.qhathia.mobile.discovery.core.domain.repository.KioskSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the in-memory kiosk exit state. */
@Module
@InstallIn(SingletonComponent::class)
abstract class KioskDataModule {

    @Binds
    @Singleton
    abstract fun bindKioskSessionRepository(impl: KioskSessionRepositoryImpl): KioskSessionRepository
}
