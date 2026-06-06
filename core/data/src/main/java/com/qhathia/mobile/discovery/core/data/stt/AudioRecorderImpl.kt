package com.qhathia.mobile.discovery.core.data.stt

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.qhathia.mobile.discovery.core.data.BuildConfig
import com.qhathia.mobile.discovery.core.domain.VoiceSearchConstants
import com.qhathia.mobile.discovery.core.domain.audio.AudioLevel
import com.qhathia.mobile.discovery.core.domain.audio.AudioRecorder
import com.qhathia.mobile.discovery.core.domain.model.RecordedAudio
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AudioRecorder] backed by `MediaRecorder`, writing a lightweight clip to an app-private temp file.
 *
 * Uses **AAC in an MP4/M4A container** (`audio/mp4`) on every supported API level. AAC is the most
 * reliable `MediaRecorder` codec across devices and is natively supported by Deepgram; the previously
 * used Ogg/Opus path produced empty/invalid files on several devices. Captures mono at 16 kHz and
 * caps the recording at [VoiceSearchConstants.MAX_RECORDING_SECONDS].
 */
@Singleton
class AudioRecorderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudioRecorder {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMillis: Long = 0L

    override suspend fun start() {
        cancel() // ensure no previous session lingers

        val file = File(context.cacheDir, "voice_search_${System.currentTimeMillis()}.m4a")
        val recorder = createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1)
            setAudioSamplingRate(SAMPLE_RATE_HZ)
            setAudioEncodingBitRate(BIT_RATE)
            setMaxDuration(VoiceSearchConstants.MAX_RECORDING_SECONDS * 1_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        this.recorder = recorder
        this.outputFile = file
        this.startedAtMillis = SystemClock.elapsedRealtime()
        log("start: recording to ${file.name} ($MIME_MP4, ${SAMPLE_RATE_HZ}Hz, ${BIT_RATE}bps)")
    }

    override fun pollLevel(): AudioLevel {
        // getMaxAmplitude() returns the peak since the previous call (the first call is 0), so this
        // is meant to be polled at a steady cadence — each reading reflects one polling window.
        val amplitude = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        return if (amplitude >= SILENCE_FLOOR) AudioLevel.Speech else AudioLevel.Silence
    }

    override suspend fun stop(): RecordedAudio? = withContext(Dispatchers.IO) {
        val recorder = recorder ?: return@withContext null
        val file = outputFile
        val durationMillis = SystemClock.elapsedRealtime() - startedAtMillis

        val stopError = runCatching { recorder.stop() }.exceptionOrNull()
        recorder.release()
        this@AudioRecorderImpl.recorder = null

        val bytes = if (stopError == null && file != null) {
            runCatching { file.readBytes() }.getOrDefault(ByteArray(0))
        } else {
            ByteArray(0)
        }
        file?.delete()
        outputFile = null

        log("stop: duration=${durationMillis}ms size=${bytes.size}B" +
            (stopError?.let { " stopError=${it.message}" } ?: ""))

        if (bytes.isEmpty()) null else RecordedAudio(bytes, MIME_MP4, durationMillis)
    }

    override fun cancel() {
        recorder?.let { active ->
            runCatching { active.stop() }
            active.release()
        }
        recorder = null
        outputFile?.delete()
        outputFile = null
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

    private fun log(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    private companion object {
        const val TAG = "VoiceSearchStt"
        const val MIME_MP4 = "audio/mp4"
        const val SAMPLE_RATE_HZ = 16_000
        const val BIT_RATE = 64_000
        const val SILENCE_FLOOR = 1_000
    }
}
