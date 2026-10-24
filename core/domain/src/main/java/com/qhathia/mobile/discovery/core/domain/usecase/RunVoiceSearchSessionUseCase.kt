package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.VoiceSearchConstants
import com.qhathia.mobile.discovery.core.domain.audio.AudioLevel
import com.qhathia.mobile.discovery.core.domain.audio.AudioRecorder
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.domain.model.TranscriptUpdate
import com.qhathia.mobile.discovery.core.domain.model.VoiceSessionEvent
import com.qhathia.mobile.discovery.core.domain.repository.LiveTranscriptionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/** One voice-search attempt. Single use: collect [events] once. */
interface VoiceSearchSession {

    /**
     * Collecting starts the attempt (microphone + live transcription); cancelling the collection
     * aborts it and releases the microphone. Completes after a terminal event.
     */
    val events: Flow<VoiceSessionEvent>

    /** Ends the recording now (consumer tapped the mic) and waits for the final transcript. */
    fun stop()
}

/**
 * Runs a voice-search attempt end to end, shared by every screen with voice search: starts the
 * microphone, streams it to live transcription, applies on-device endpointing (30s input wait, 45s
 * cap, 1s trailing silence after speech) and finalizes. Enforces the final-response (120s) and total
 * (150s) budgets; the connection budget is enforced by the network layer.
 */
class RunVoiceSearchSessionUseCase @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val liveTranscription: LiveTranscriptionRepository,
) {
    operator fun invoke(): VoiceSearchSession = Session(audioRecorder, liveTranscription)
}

private sealed interface Input {
    data object Tick : Input
    data object Stop : Input
    data object ResponseTimeout : Input
    data class Update(val update: TranscriptUpdate) : Input
    data class TranscriptionFailed(val category: SttErrorCategory) : Input
    data object TranscriptionEnded : Input
}

private class Session(
    private val audioRecorder: AudioRecorder,
    private val liveTranscription: LiveTranscriptionRepository,
) : VoiceSearchSession {

    /** Every input (ticks, transcript updates, user stop) is handled by one loop, in order. */
    private val inputs = Channel<Input>(Channel.UNLIMITED)

    override fun stop() {
        inputs.trySend(Input.Stop)
    }

    override val events: Flow<VoiceSessionEvent> = channelFlow {
        try {
            withTimeout(VoiceSearchConstants.TOTAL_OPERATION_TIMEOUT_MS) { runSession() }
        } catch (timeout: TimeoutCancellationException) {
            send(VoiceSessionEvent.Failed(SttErrorCategory.Timeout))
        } finally {
            // No-op after a normal stop; releases the microphone on cancel, failure or timeout.
            audioRecorder.cancel()
        }
    }

    private suspend fun ProducerScope<VoiceSessionEvent>.runSession() = coroutineScope {
        try {
            audioRecorder.start()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            send(VoiceSessionEvent.Failed((error as? SttException)?.category ?: SttErrorCategory.Generic))
            return@coroutineScope
        }

        launch { forwardTranscription() }
        val ticker = launch {
            while (true) {
                delay(VoiceSearchConstants.LEVEL_POLL_INTERVAL_MS)
                inputs.send(Input.Tick)
            }
        }

        val pollMs = VoiceSearchConstants.LEVEL_POLL_INTERVAL_MS
        val inputWaitMs = VoiceSearchConstants.INPUT_WAIT_SECONDS * ONE_SECOND_MS
        val maxRecordingMs = VoiceSearchConstants.MAX_RECORDING_SECONDS * ONE_SECOND_MS

        var recording = true
        var elapsedMs = 0L
        var hasSpoken = false
        var trailingSilenceMs = 0L
        var transcript = ""
        var finalTranscript: String? = null
        var lastListening = VoiceSessionEvent.Listening("", VoiceSearchConstants.MAX_RECORDING_SECONDS)
        send(lastListening)

        suspend fun emitListening() {
            val remaining = ((maxRecordingMs - elapsedMs) / ONE_SECOND_MS).toInt().coerceAtLeast(0)
            val next = VoiceSessionEvent.Listening(transcript, remaining)
            if (next != lastListening) {
                lastListening = next
                send(next)
            }
        }

        var lastFinalizing: VoiceSessionEvent.Finalizing? = null

        // Results lag behind the audio, so most of the transcript can arrive after the recording ends.
        suspend fun emitFinalizing() {
            val next = VoiceSessionEvent.Finalizing(transcript)
            if (next != lastFinalizing) {
                lastFinalizing = next
                send(next)
            }
        }

        suspend fun finishRecording() {
            recording = false
            ticker.cancel()
            audioRecorder.stop() // completes the chunk stream, which closes the live stream
            emitFinalizing()
            launch {
                delay(VoiceSearchConstants.RESPONSE_TIMEOUT_MS)
                inputs.send(Input.ResponseTimeout)
            }
        }

        val outcome: VoiceSessionEvent = run {
            for (input in inputs) {
                when (input) {
                    Input.Tick -> {
                        if (!recording) continue
                        elapsedMs += pollMs
                        when (audioRecorder.pollLevel()) {
                            AudioLevel.Speech -> {
                                hasSpoken = true
                                trailingSilenceMs = 0
                            }
                            AudioLevel.Silence -> if (hasSpoken) trailingSilenceMs += pollMs
                        }
                        when {
                            hasSpoken && trailingSilenceMs >= VoiceSearchConstants.SILENCE_AUTO_STOP_MS ->
                                finishRecording()
                            !hasSpoken && elapsedMs >= inputWaitMs -> return@run VoiceSessionEvent.NoAudio
                            elapsedMs >= maxRecordingMs -> finishRecording()
                            else -> emitListening()
                        }
                    }
                    Input.Stop -> if (recording) finishRecording()
                    is Input.Update -> when (val update = input.update) {
                        is TranscriptUpdate.Partial -> {
                            transcript = update.text
                            if (recording) emitListening() else emitFinalizing()
                        }
                        is TranscriptUpdate.Final -> finalTranscript = update.text
                    }
                    is Input.TranscriptionFailed -> return@run VoiceSessionEvent.Failed(input.category)
                    Input.TranscriptionEnded -> {
                        // The stream must only end after the recording does; earlier means it dropped.
                        if (recording) return@run VoiceSessionEvent.Failed(SttErrorCategory.TranscriptionResponse)
                        val text = finalTranscript?.trim().orEmpty()
                        return@run if (text.isEmpty()) {
                            VoiceSessionEvent.Failed(SttErrorCategory.EmptyAudio)
                        } else {
                            VoiceSessionEvent.Transcribed(text)
                        }
                    }
                    Input.ResponseTimeout -> return@run VoiceSessionEvent.Failed(SttErrorCategory.Timeout)
                }
            }
            VoiceSessionEvent.Failed(SttErrorCategory.Generic)
        }
        send(outcome)
        coroutineContext.cancelChildren()
    }

    private suspend fun forwardTranscription() {
        try {
            liveTranscription.transcribe(audioRecorder.audioChunks()).collect { inputs.send(Input.Update(it)) }
            inputs.send(Input.TranscriptionEnded)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            val category = (error as? SttException)?.category ?: SttErrorCategory.Generic
            inputs.send(Input.TranscriptionFailed(category))
        }
    }

    private companion object {
        const val ONE_SECOND_MS = 1_000L
    }
}
