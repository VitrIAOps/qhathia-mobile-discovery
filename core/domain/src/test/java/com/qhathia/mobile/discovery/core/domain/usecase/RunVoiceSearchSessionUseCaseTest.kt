package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.audio.AudioLevel
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.domain.model.VoiceSessionEvent
import com.qhathia.mobile.discovery.core.testing.FakeAudioRecorder
import com.qhathia.mobile.discovery.core.testing.FakeLiveTranscriptionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RunVoiceSearchSessionUseCaseTest {

    private val recorder = FakeAudioRecorder()
    private val transcription = FakeLiveTranscriptionRepository()
    private val useCase = RunVoiceSearchSessionUseCase(recorder, transcription)

    private class Collected(val events: MutableList<VoiceSessionEvent>, val job: Job)

    private fun TestScope.collect(session: VoiceSearchSession): Collected {
        val events = mutableListOf<VoiceSessionEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { session.events.toList(events) }
        return Collected(events, job)
    }

    @Test
    fun given_interim_results_when_listening_then_listening_events_carry_the_transcript() = runTest {
        val collected = collect(useCase())
        runCurrent()

        val listening = collected.events.filterIsInstance<VoiceSessionEvent.Listening>()
        assertEquals("", listening.first().transcript)
        assertEquals("taladro de la marca", listening.last().transcript)
        assertTrue(recorder.isRecording)
        collected.job.cancel()
    }

    @Test
    fun given_trailing_silence_after_speech_then_finalizes_and_transcribes() = runTest {
        val collected = collect(useCase())
        advanceTimeBy(600) // the consumer speaks
        recorder.level = AudioLevel.Silence
        advanceUntilIdle()

        assertTrue(recorder.stopped)
        assertTrue(collected.events.any { it is VoiceSessionEvent.Finalizing })
        assertEquals(VoiceSessionEvent.Transcribed("taladro de la marca bosch"), collected.events.last())
    }

    @Test
    fun given_no_speech_when_input_wait_elapses_then_no_audio() = runTest {
        recorder.level = AudioLevel.Silence
        val collected = collect(useCase())
        advanceTimeBy(29_900)
        runCurrent()
        assertFalse(collected.events.last() is VoiceSessionEvent.NoAudio)

        advanceUntilIdle()

        assertEquals(VoiceSessionEvent.NoAudio, collected.events.last())
        assertTrue(recorder.cancelled)
    }

    @Test
    fun given_continuous_speech_when_cap_reached_then_finalizes() = runTest {
        val collected = collect(useCase())
        advanceTimeBy(44_900)
        runCurrent()
        assertFalse(collected.events.any { it is VoiceSessionEvent.Finalizing })

        advanceUntilIdle()

        assertTrue(collected.events.any { it is VoiceSessionEvent.Finalizing })
        assertEquals(VoiceSessionEvent.Transcribed("taladro de la marca bosch"), collected.events.last())
    }

    @Test
    fun given_results_arrive_after_recording_ends_then_finalizing_carries_the_late_transcript() = runTest {
        transcription.partials = emptyList()
        transcription.latePartials = listOf("amola", "amoladora")
        transcription.finalText = "amoladora"
        transcription.finalDelayMillis = 500
        val session = useCase()
        val collected = collect(session)
        runCurrent()

        session.stop()
        runCurrent()

        val finalizing = collected.events.filterIsInstance<VoiceSessionEvent.Finalizing>()
        assertEquals(listOf("", "amola", "amoladora"), finalizing.map { it.transcript })
        assertTrue(collected.events.filterIsInstance<VoiceSessionEvent.Listening>().all { it.transcript.isEmpty() })

        advanceUntilIdle()
        assertEquals(VoiceSessionEvent.Transcribed("amoladora"), collected.events.last())
    }

    @Test
    fun given_transcript_while_recording_when_stopped_then_finalizing_starts_with_it() = runTest {
        val session = useCase()
        val collected = collect(session)
        runCurrent()

        session.stop()
        runCurrent()

        assertEquals(
            VoiceSessionEvent.Finalizing("taladro de la marca"),
            collected.events.first { it is VoiceSessionEvent.Finalizing },
        )
    }

    @Test
    fun given_listening_when_stopped_then_finalizes_without_waiting_for_silence() = runTest {
        val session = useCase()
        val collected = collect(session)
        runCurrent()

        session.stop()
        runCurrent()

        assertTrue(recorder.stopped)
        assertEquals(VoiceSessionEvent.Transcribed("taladro de la marca bosch"), collected.events.last())
    }

    @Test
    fun given_stream_fails_while_recording_then_failed_and_recorder_released() = runTest {
        transcription.failure = SttException(SttErrorCategory.Network)
        val collected = collect(useCase())
        runCurrent()

        assertEquals(VoiceSessionEvent.Failed(SttErrorCategory.Network), collected.events.last())
        assertTrue(recorder.cancelled)
    }

    @Test
    fun given_stream_ends_before_recording_stops_then_failed() = runTest {
        transcription.endEarly = true
        val collected = collect(useCase())
        runCurrent()

        assertEquals(VoiceSessionEvent.Failed(SttErrorCategory.TranscriptionResponse), collected.events.last())
    }

    @Test
    fun given_blank_final_transcript_then_failed_empty_audio() = runTest {
        transcription.finalText = "   "
        val session = useCase()
        val collected = collect(session)
        session.stop()
        runCurrent()

        assertEquals(VoiceSessionEvent.Failed(SttErrorCategory.EmptyAudio), collected.events.last())
    }

    @Test
    fun given_final_transcript_never_arrives_then_failed_timeout() = runTest {
        transcription.finalDelayMillis = 200_000
        val session = useCase()
        val collected = collect(session)
        session.stop()
        advanceUntilIdle()

        assertEquals(VoiceSessionEvent.Failed(SttErrorCategory.Timeout), collected.events.last())
    }

    @Test
    fun given_microphone_fails_to_start_then_failed_without_transcribing() = runTest {
        recorder.startFailure = SttException(SttErrorCategory.Generic)
        val collected = collect(useCase())
        runCurrent()

        assertEquals(listOf(VoiceSessionEvent.Failed(SttErrorCategory.Generic)), collected.events)
        assertEquals(0, transcription.calls)
    }

    @Test
    fun given_listening_when_collection_cancelled_then_recorder_cancelled() = runTest {
        val collected = collect(useCase())
        runCurrent()

        collected.job.cancel()
        runCurrent()

        assertTrue(recorder.cancelled)
    }

    @Test
    fun given_chunks_captured_then_they_reach_the_transcription_stream() = runTest {
        val session = useCase()
        val collected = collect(session)
        recorder.emitChunk()
        recorder.emitChunk()
        session.stop()
        runCurrent()

        assertEquals(2, transcription.receivedChunks)
        assertTrue(collected.events.last() is VoiceSessionEvent.Transcribed)
    }
}
