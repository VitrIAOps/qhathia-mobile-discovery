package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.network.ConnectivityDataSource
import com.qhathia.mobile.discovery.core.data.network.ConnectivityDataSourceImpl
import com.qhathia.mobile.discovery.core.data.network.NetworkSettingsRepositoryImpl
import com.qhathia.mobile.discovery.core.data.network.WifiDataSource
import com.qhathia.mobile.discovery.core.data.network.WifiDataSourceImpl
import com.qhathia.mobile.discovery.core.domain.repository.NetworkSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the network-settings repository and its Android-backed data sources into the singleton graph. */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkSettingsDataModule {

    @Binds
    @Singleton
    abstract fun bindNetworkSettingsRepository(
        impl: NetworkSettingsRepositoryImpl,
    ): NetworkSettingsRepository

    @Binds
    @Singleton
    abstract fun bindWifiDataSource(impl: WifiDataSourceImpl): WifiDataSource

    @Binds
    @Singleton
    abstract fun bindConnectivityDataSource(impl: ConnectivityDataSourceImpl): ConnectivityDataSource
}
