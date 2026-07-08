package com.qhathia.mobile.discovery.core.network.di

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.KtorClientFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

/**
 * Provides the singleton Ktor [HttpClient]. The API services ([SttTokenApiService], [DeepgramClient],
 * [DevicePairingApiService]) are constructor-injected, so they need no explicit `@Provides`.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient =
        // Debug builds always log; a non-debuggable build can opt in via ENABLE_NETWORK_LOGGING
        // (set on the `profile` build type) so API call logs survive in QA/profiling APKs.
        KtorClientFactory.create(enableLogging = BuildConfig.DEBUG || BuildConfig.ENABLE_NETWORK_LOGGING)
}
