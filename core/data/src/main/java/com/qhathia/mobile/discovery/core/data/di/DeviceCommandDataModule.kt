package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.command.AppRestarterImpl
import com.qhathia.mobile.discovery.core.data.command.CatalogRefreshSignalImpl
import com.qhathia.mobile.discovery.core.data.command.DeviceCommandRepositoryImpl
import com.qhathia.mobile.discovery.core.data.command.PendingRestartAckRepositoryImpl
import com.qhathia.mobile.discovery.core.domain.repository.AppRestarter
import com.qhathia.mobile.discovery.core.domain.repository.CatalogRefreshSignal
import com.qhathia.mobile.discovery.core.domain.repository.DeviceCommandRepository
import com.qhathia.mobile.discovery.core.domain.repository.PendingRestartAckRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the remote-command ack repository, the catalog refresh signal, the app restarter and its pending ack. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceCommandDataModule {

    @Binds
    @Singleton
    abstract fun bindDeviceCommandRepository(impl: DeviceCommandRepositoryImpl): DeviceCommandRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRefreshSignal(impl: CatalogRefreshSignalImpl): CatalogRefreshSignal

    @Binds
    @Singleton
    abstract fun bindAppRestarter(impl: AppRestarterImpl): AppRestarter

    @Binds
    abstract fun bindPendingRestartAckRepository(impl: PendingRestartAckRepositoryImpl): PendingRestartAckRepository
}
