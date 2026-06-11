package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.pairing.DevicePairingRepositoryImpl
import com.qhathia.mobile.discovery.core.domain.repository.DevicePairingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the device pairing repository implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DevicePairingDataModule {

    @Binds
    @Singleton
    abstract fun bindDevicePairingRepository(
        impl: DevicePairingRepositoryImpl,
    ): DevicePairingRepository
}
