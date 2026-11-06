package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.unlink.DeviceUnlinkEventsImpl
import com.qhathia.mobile.discovery.core.domain.repository.DeviceUnlinkEvents
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the "tablet was unlinked" events raised by the HTTP client. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceUnlinkDataModule {

    @Binds
    @Singleton
    abstract fun bindDeviceUnlinkEvents(impl: DeviceUnlinkEventsImpl): DeviceUnlinkEvents
}
