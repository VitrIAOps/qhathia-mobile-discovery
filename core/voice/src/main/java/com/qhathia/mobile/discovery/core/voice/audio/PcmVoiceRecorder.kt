package com.qhathia.mobile.discovery.core.voice.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import com.qhathia.mobile.discovery.core.voice.VoiceLogger
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

/** Whether the last polling window had speech-level audio. */
enum class VoiceLevel { Speech, Silence }

/** The microphone could not start recording. */
class VoiceCaptureException(reason: String?) : Exception(reason)

/**
 * Microphone capture backed by `AudioRecord`, producing raw PCM (16-bit little-endian, 16 kHz, mono)
 * for live streaming.
 *
 * A read loop on [Dispatchers.IO] reads ~100 ms buffers, pushes each one into an unbounded channel
 * (so audio captured while the live connection is still opening is kept) and tracks the peak sample
 * since the last [pollLevel]. The loop owns the `AudioRecord` and releases it when it exits.
 *
 * The host app declares `RECORD_AUDIO` and must hold the grant before calling [start].
 */
class PcmVoiceRecorder(private val logger: VoiceLogger = VoiceLogger.None) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val peak = AtomicInteger(0)

    private var record: AudioRecord? = null
    private var chunks: Channel<ByteArray>? = null
    private var readJob: Job? = null
    private var startedAtMillis: Long = 0L
    private var capturedBytes: Long = 0L

    /** @throws VoiceCaptureException when the microphone cannot start. */
    @SuppressLint("MissingPermission")
    fun start() {
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
        logger.debug("start: PCM ${SAMPLE_RATE_HZ}Hz mono 16-bit")
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

    fun audioChunks(): Flow<ByteArray> = chunks?.receiveAsFlow() ?: emptyFlow()

    fun pollLevel(): VoiceLevel =
        if (peak.getAndSet(0) >= SILENCE_FLOOR) VoiceLevel.Speech else VoiceLevel.Silence

    suspend fun stop() {
        val record = record ?: return
        val channel = chunks
        val job = readJob
        clearSession()

        runCatching { record.stop() }
        job?.cancelAndJoin() // the loop has pushed its last chunk and released the recorder
        channel?.close()
        logger.debug("stop: duration=${SystemClock.elapsedRealtime() - startedAtMillis}ms size=${capturedBytes}B")
    }

    fun cancel() {
        val record = record ?: return
        val channel = chunks
        val job = readJob
        clearSession()

        runCatching { record.stop() }
        job?.cancel()
        channel?.cancel(CancellationException("Recording cancelled"))
        logger.debug("cancel")
    }

    private fun clearSession() {
        record = null
        chunks = null
        readJob = null
    }

    private fun startFailure(reason: String?): VoiceCaptureException {
        logger.debug("start failed: $reason")
        return VoiceCaptureException(reason)
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 16_000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        /** ~100 ms of 16 kHz 16-bit mono audio. */
        const val CHUNK_BYTES = 3_200

        /** Peak sample (0–32767) at or above which a polling window counts as speech. */
        const val SILENCE_FLOOR = 1_000
    }
}
