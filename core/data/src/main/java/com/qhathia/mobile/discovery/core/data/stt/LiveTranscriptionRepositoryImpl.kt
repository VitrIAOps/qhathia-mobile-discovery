package com.qhathia.mobile.discovery.core.data.stt

import android.util.Log
import com.qhathia.mobile.discovery.core.data.BuildConfig
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.domain.model.TranscriptUpdate
import com.qhathia.mobile.discovery.core.domain.repository.LiveTranscriptionRepository
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.DeepgramLiveClient
import com.qhathia.mobile.discovery.core.network.service.DeepgramStreamException
import com.qhathia.mobile.discovery.core.network.service.SttTokenApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

/**
 * Live transcription pipeline: ensure the Qhathia JWT → fetch an ephemeral Deepgram token → stream
 * the audio to Deepgram Live → turn its results into [TranscriptUpdate]s. Every failure surfaces as
 * an [SttException] tagged with an [SttErrorCategory]. The ephemeral token is used once and is never
 * logged or retained.
 */
class LiveTranscriptionRepositoryImpl @Inject constructor(
    private val sessionTokenProvider: SessionTokenProvider,
    private val sttTokenApiService: SttTokenApiService,
    private val deepgramLiveClient: DeepgramLiveClient,
) : LiveTranscriptionRepository {

    override fun transcribe(audio: Flow<ByteArray>): Flow<TranscriptUpdate> = flow {
        val timing = SessionTiming()
        val accessToken = sessionTokenProvider.accessToken().getOrElse { error ->
            throw failure((error as? SttException)?.category ?: SttErrorCategory.Generic)
        }
        val deepgramToken = when (val tokenResult = sttTokenApiService.getToken(accessToken)) {
            is NetworkResult.Success -> tokenResult.data.token
            is NetworkResult.HttpError -> throw failure(SttErrorMapper.fromTokenHttpError(tokenResult.code))
            is NetworkResult.NetworkError -> throw failure(SttErrorMapper.fromNetworkError(tokenResult.kind))
        }

        log("STT session: open (linear16, 16000Hz, mono) after ${timing.sinceStartMs()}ms")
        val accumulator = TranscriptAccumulator()
        val trackedAudio = audio.onCompletion { timing.markAudioEnded() }
        try {
            deepgramLiveClient.stream(trackedAudio, deepgramToken).collect { result ->
                accumulator.apply(result.transcript, result.isFinal)?.let { text ->
                    log("STT partial: ${timing.onPartial()} '$text'")
                    emit(TranscriptUpdate.Partial(text))
                }
            }
            log("STT timing: ${timing.summary()}")
        } catch (stream: DeepgramStreamException) {
            log(
                "STT session: failed closeCode=${stream.closeCode} " +
                    "handshakeRejected=${stream.handshakeRejected} kind=${stream.kind}",
            )
            throw failure(SttErrorMapper.fromStreamFailure(stream.closeCode, stream.handshakeRejected, stream.kind))
        }

        val transcript = accumulator.transcript
        if (transcript.isBlank()) throw failure(SttErrorCategory.EmptyAudio)
        log("STT result: transcript='$transcript'")
        emit(TranscriptUpdate.Final(transcript))
    }

    private fun failure(category: SttErrorCategory): SttException {
        log("STT result: failure=$category")
        return SttException(category)
    }

    private fun log(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    private companion object {
        const val TAG = "VoiceSearchStt"
    }
}

/**
 * Debug timing for one session: when the first partial arrived and how many arrived after the
 * recording ended. The audio end is marked from the socket's sender coroutine, hence the atomics.
 */
private class SessionTiming {
    private val startedAtNanos = System.nanoTime()
    private val audioEndedAtNanos = AtomicLong(NOT_SET)
    private var partials = 0
    private var latePartials = 0
    private var firstPartialMs: Long? = null

    fun sinceStartMs(): Long = (System.nanoTime() - startedAtNanos) / NANOS_PER_MS

    fun markAudioEnded() {
        audioEndedAtNanos.compareAndSet(NOT_SET, System.nanoTime())
    }

    /** Records one partial and describes it, e.g. `#2 at 1840ms (after stop +320ms)`. */
    fun onPartial(): String {
        partials++
        val atMs = sinceStartMs()
        if (firstPartialMs == null) firstPartialMs = atMs
        val endedAt = audioEndedAtNanos.get()
        val late = if (endedAt == NOT_SET) {
            ""
        } else {
            latePartials++
            " (after stop +${(System.nanoTime() - endedAt) / NANOS_PER_MS}ms)"
        }
        return "#$partials at ${atMs}ms$late"
    }

    fun summary(): String {
        val endedAt = audioEndedAtNanos.get()
        val finalAfterStop = if (endedAt == NOT_SET) "n/a" else "${(System.nanoTime() - endedAt) / NANOS_PER_MS}ms"
        return "firstPartial=${firstPartialMs?.let { "${it}ms" } ?: "none"} partials=$partials " +
            "afterStop=$latePartials finalAfterStop=$finalAfterStop"
    }

    private companion object {
        const val NOT_SET = -1L
        const val NANOS_PER_MS = 1_000_000L
    }
}
