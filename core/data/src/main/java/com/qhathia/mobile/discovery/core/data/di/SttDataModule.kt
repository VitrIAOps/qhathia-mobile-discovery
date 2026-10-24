package com.qhathia.mobile.discovery.core.data.di

import com.qhathia.mobile.discovery.core.data.stt.AudioRecorderImpl
import com.qhathia.mobile.discovery.core.data.stt.LiveTranscriptionRepositoryImpl
import com.qhathia.mobile.discovery.core.domain.audio.AudioRecorder
import com.qhathia.mobile.discovery.core.domain.repository.LiveTranscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the voice-search STT implementations (live transcription repository + microphone recorder). */
@Module
@InstallIn(SingletonComponent::class)
abstract class SttDataModule {

    @Binds
    @Singleton
    abstract fun bindLiveTranscriptionRepository(impl: LiveTranscriptionRepositoryImpl): LiveTranscriptionRepository

    @Binds
    @Singleton
    abstract fun bindAudioRecorder(impl: AudioRecorderImpl): AudioRecorder
}
