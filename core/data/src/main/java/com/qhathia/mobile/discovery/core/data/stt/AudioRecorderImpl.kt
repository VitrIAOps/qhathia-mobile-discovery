package com.qhathia.mobile.discovery.core.data.stt

import android.util.Log
import com.qhathia.mobile.discovery.core.data.BuildConfig
import com.qhathia.mobile.discovery.core.domain.audio.AudioLevel
import com.qhathia.mobile.discovery.core.domain.audio.AudioRecorder
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.voice.VoiceLogger
import com.qhathia.mobile.discovery.core.voice.audio.PcmVoiceRecorder
import com.qhathia.mobile.discovery.core.voice.audio.VoiceCaptureException
import com.qhathia.mobile.discovery.core.voice.audio.VoiceLevel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** [AudioRecorder] over `core:voice`'s microphone capture, translating its levels and failures. */
@Singleton
class AudioRecorderImpl @Inject constructor() : AudioRecorder {

    private val recorder = PcmVoiceRecorder(
        VoiceLogger { message -> if (BuildConfig.DEBUG) Log.d(TAG, message) },
    )

    override suspend fun start() {
        try {
            recorder.start()
        } catch (_: VoiceCaptureException) {
            throw SttException(SttErrorCategory.Generic)
        }
    }

    override fun audioChunks(): Flow<ByteArray> = recorder.audioChunks()

    override fun pollLevel(): AudioLevel = when (recorder.pollLevel()) {
        VoiceLevel.Speech -> AudioLevel.Speech
        VoiceLevel.Silence -> AudioLevel.Silence
    }

    override suspend fun stop() = recorder.stop()

    override fun cancel() = recorder.cancel()

    private companion object {
        const val TAG = "VoiceSearchStt"
    }
}
