package com.qhathia.mobile.discovery.core.network.di

import com.qhathia.mobile.discovery.core.network.BuildConfig
import com.qhathia.mobile.discovery.core.network.DeviceUnlinkedSignal
import com.qhathia.mobile.discovery.core.network.KtorClientFactory
import com.qhathia.mobile.discovery.core.voice.transcription.DeepgramConfig
import com.qhathia.mobile.discovery.core.voice.transcription.LiveTranscriber
import com.qhathia.mobile.discovery.core.voice.transcription.deepgramLiveTranscriber
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

/**
 * Provides the singleton Ktor [HttpClient]. The API services ([SttTokenApiService],
 * [DevicePairingApiService]) are constructor-injected, so they need no explicit `@Provides`.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(deviceUnlinkedSignal: DeviceUnlinkedSignal): HttpClient =
        // Debug builds always log; a non-debuggable build can opt in via ENABLE_NETWORK_LOGGING
        // (set on the `profile` build type) so API call logs survive in QA/profiling APKs.
        KtorClientFactory.create(
            enableLogging = BuildConfig.DEBUG || BuildConfig.ENABLE_NETWORK_LOGGING,
            onDeviceUnlinked = deviceUnlinkedSignal::notifyUnlinked,
        )

    /** Speech-to-text over the same client, so voice traffic keeps the app's HTTP configuration. */
    @Provides
    @Singleton
    fun provideLiveTranscriber(client: HttpClient): LiveTranscriber =
        deepgramLiveTranscriber(
            client = client,
            config = DeepgramConfig(
                baseUrl = BuildConfig.DEEPGRAM_BASE_URL,
                model = "nova-3",
                language = "es",
            ),
        )
}
