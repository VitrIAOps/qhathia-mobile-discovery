package com.qhathia.mobile.discovery.core.data.di

import android.content.Context
import com.qhathia.mobile.discovery.core.device.connectivity.ConnectivityDataSource
import com.qhathia.mobile.discovery.core.device.connectivity.ConnectivityDataSourceImpl
import com.qhathia.mobile.discovery.core.data.network.NetworkSettingsRepositoryImpl
import com.qhathia.mobile.discovery.core.device.connectivity.WifiDataSource
import com.qhathia.mobile.discovery.core.device.connectivity.WifiDataSourceImpl
import com.qhathia.mobile.discovery.core.domain.repository.NetworkSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    companion object {
        @Provides
        @Singleton
        fun provideWifiDataSource(@ApplicationContext context: Context): WifiDataSource =
            WifiDataSourceImpl(context)

        @Provides
        @Singleton
        fun provideConnectivityDataSource(@ApplicationContext context: Context): ConnectivityDataSource =
            ConnectivityDataSourceImpl(context)
    }
}
