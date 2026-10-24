package com.qhathia.mobile.discovery.core.data.stt

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import com.qhathia.mobile.discovery.core.data.BuildConfig
import com.qhathia.mobile.discovery.core.domain.audio.AudioLevel
import com.qhathia.mobile.discovery.core.domain.audio.AudioRecorder
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AudioRecorder] backed by `AudioRecord`, producing raw PCM (16-bit little-endian, 16 kHz, mono)
 * for live streaming to Deepgram.
 *
 * A read loop on [Dispatchers.IO] reads ~100 ms buffers, pushes each one into an unbounded channel
 * (so audio captured while the live connection is still opening is kept) and tracks the peak sample
 * since the last [pollLevel]. The loop owns the `AudioRecord` and releases it when it exits.
 */
@Singleton
class AudioRecorderImpl @Inject constructor() : AudioRecorder {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val peak = AtomicInteger(0)

    private var record: AudioRecord? = null
    private var chunks: Channel<ByteArray>? = null
    private var readJob: Job? = null
    private var startedAtMillis: Long = 0L
    private var capturedBytes: Long = 0L

    // Voice search is gated on the RECORD_AUDIO grant (requested at app open) before start() is called.
    @SuppressLint("MissingPermission")
    override suspend fun start() {
        cancel() // ensure no previous session lingers

        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL_CONFIG, ENCODING)
        if (minBuffer <= 0) throw startFailure("getMinBufferSize=$minBuffer")
        val record = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE_HZ,
                CHANNEL_CONFIG,
                ENCODING,
                maxOf(minBuffer, CHUNK_BYTES * 2),
            )
        }.getOrElse { throw startFailure(it.message) }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw startFailure("not initialized")
        }
        runCatching { record.startRecording() }
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            record.release()
            throw startFailure("startRecording failed (microphone busy?)")
        }

        val channel = Channel<ByteArray>(Channel.UNLIMITED)
        peak.set(0)
        capturedBytes = 0L
        startedAtMillis = SystemClock.elapsedRealtime()
        this.record = record
        this.chunks = channel
        readJob = scope.launch { readLoop(record, channel) }
        log("start: PCM ${SAMPLE_RATE_HZ}Hz mono 16-bit")
    }

    private fun CoroutineScope.readLoop(record: AudioRecord, channel: Channel<ByteArray>) {
        val buffer = ByteArray(CHUNK_BYTES)
        try {
            while (isActive) {
                val read = record.read(buffer, 0, buffer.size) // blocks; record.stop() unblocks it
                if (read < 0) break // AudioRecord error code
                if (read == 0) continue
                peak.accumulateAndGet(PcmLevels.peakAmplitude(buffer, read)) { a, b -> maxOf(a, b) }
                capturedBytes += read
                channel.trySend(buffer.copyOf(read))
            }
        } finally {
            record.release()
        }
    }

    override fun audioChunks(): Flow<ByteArray> = chunks?.receiveAsFlow() ?: emptyFlow()

    override fun pollLevel(): AudioLevel =
        if (peak.getAndSet(0) >= SILENCE_FLOOR) AudioLevel.Speech else AudioLevel.Silence

    override suspend fun stop() {
        val record = record ?: return
        val channel = chunks
        val job = readJob
        clearSession()

        runCatching { record.stop() }
        job?.cancelAndJoin() // the loop has pushed its last chunk and released the recorder
        channel?.close()
        log("stop: duration=${SystemClock.elapsedRealtime() - startedAtMillis}ms size=${capturedBytes}B")
    }

    override fun cancel() {
        val record = record ?: return
        val channel = chunks
        val job = readJob
        clearSession()

        runCatching { record.stop() }
        job?.cancel()
        channel?.cancel(CancellationException("Recording cancelled"))
        log("cancel")
    }

    private fun clearSession() {
        record = null
        chunks = null
        readJob = null
    }

    private fun startFailure(reason: String?): SttException {
        log("start failed: $reason")
        return SttException(SttErrorCategory.Generic)
    }

    private fun log(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    private companion object {
        const val TAG = "VoiceSearchStt"
        const val SAMPLE_RATE_HZ = 16_000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        /** ~100 ms of 16 kHz 16-bit mono audio. */
        const val CHUNK_BYTES = 3_200

        /** Peak sample (0–32767) at or above which a polling window counts as speech. */
        const val SILENCE_FLOOR = 1_000
    }
}
