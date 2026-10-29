package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.common.ElapsedRealtimeClock
import com.qhathia.mobile.discovery.core.common.SystemElapsedRealtimeClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Provides the platform clocks. */
@Module
@InstallIn(SingletonComponent::class)
object ClockDataModule {

    @Provides
    fun provideElapsedRealtimeClock(): ElapsedRealtimeClock = SystemElapsedRealtimeClock()
}
