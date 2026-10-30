package com.qhathia.mobile.discovery.feature.home

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.VoiceSearchConstants
import com.qhathia.mobile.discovery.core.domain.audio.AudioLevel
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RunVoiceSearchSessionUseCase
import com.qhathia.mobile.discovery.core.testing.FakeAudioRecorder
import com.qhathia.mobile.discovery.core.testing.FakeLiveTranscriptionRepository
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.feature.home.model.HomeUiEffect
import com.qhathia.mobile.discovery.feature.home.model.HomeUiEvent
import com.qhathia.mobile.discovery.feature.home.model.MicState
import com.qhathia.mobile.discovery.feature.home.ui.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private fun viewModel(
        repository: FakeStoreSessionRepository = FakeStoreSessionRepository(),
        recorder: FakeAudioRecorder = FakeAudioRecorder(),
        stt: FakeLiveTranscriptionRepository = FakeLiveTranscriptionRepository(),
    ) = HomeViewModel(
        observeStoreSession = ObserveStoreSessionUseCase(repository),
        runVoiceSearchSession = RunVoiceSearchSessionUseCase(recorder, stt),
    )

    @Test
    fun given_store_session_has_name_when_observed_then_store_name_surfaces() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel(FakeStoreSessionRepository(StoreSession(storeName = "Joyería María")))

            assertEquals("Joyería María", vm.uiState.value.storeName)
        }

    @Test
    fun given_idle_when_activate_mic_then_controls_hidden_and_listening() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()

            vm.onEvent(HomeUiEvent.ActivateMic)

            assertTrue(vm.uiState.value.micState is MicState.Listening)
            assertFalse(vm.uiState.value.areControlsVisible)
        }

    @Test
    fun given_listening_when_words_recognized_then_live_transcript_in_listening_state() =
        runTest(mainDispatcher.dispatcher) {
            val stt = FakeLiveTranscriptionRepository(partials = listOf("busco", "busco un anillo"))
            val vm = viewModel(stt = stt)

            vm.onEvent(HomeUiEvent.ActivateMic)

            val micState = vm.uiState.value.micState as MicState.Listening
            assertEquals("busco un anillo", micState.liveTranscript)
            vm.onEvent(HomeUiEvent.CancelMic)
        }

    @Test
    fun given_listening_when_nothing_recognized_then_live_transcript_is_blank() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel(stt = FakeLiveTranscriptionRepository(partials = emptyList()))

            vm.onEvent(HomeUiEvent.ActivateMic)

            assertEquals("", (vm.uiState.value.micState as MicState.Listening).liveTranscript)
            vm.onEvent(HomeUiEvent.CancelMic)
        }

    @Test
    fun given_live_stream_drops_while_listening_then_error_state() =
        runTest(mainDispatcher.dispatcher) {
            val stt = FakeLiveTranscriptionRepository(failure = SttException(SttErrorCategory.Network))
            val vm = viewModel(stt = stt)

            vm.onEvent(HomeUiEvent.ActivateMic)
            runCurrent()

            assertEquals(MicState.Error(SttErrorCategory.Network), vm.uiState.value.micState)
        }

    @Test
    fun given_recording_when_stopped_then_transcribes_to_success_and_emits_transcript() =
        runTest(mainDispatcher.dispatcher) {
            val stt = FakeLiveTranscriptionRepository(finalText = "taladro bosch")
            val vm = viewModel(stt = stt)

            vm.uiEffect.test {
                vm.onEvent(HomeUiEvent.ActivateMic)
                vm.onEvent(HomeUiEvent.StopRecording)
                advanceUntilIdle()

                assertEquals(MicState.Success("taladro bosch"), vm.uiState.value.micState)
                assertEquals(
                    HomeUiEffect.NavigateToProductSearch("taladro bosch", SearchSource.Voice),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun given_silence_after_speech_then_auto_stops_and_transcribes() =
        runTest(mainDispatcher.dispatcher) {
            val recorder = FakeAudioRecorder(level = AudioLevel.Speech)
            val stt = FakeLiveTranscriptionRepository(finalText = "taladro")
            val vm = viewModel(recorder = recorder, stt = stt)

            vm.onEvent(HomeUiEvent.ActivateMic)
            advanceTimeBy(600) // the consumer speaks
            recorder.level = AudioLevel.Silence
            advanceTimeBy(1_200) // ≥ 1s of trailing silence triggers end-of-speech
            advanceUntilIdle()

            assertEquals(MicState.Success("taladro"), vm.uiState.value.micState)
        }

    @Test
    fun given_no_detected_audio_when_input_wait_elapses_then_no_audio_retry() =
        runTest(mainDispatcher.dispatcher) {
            val recorder = FakeAudioRecorder(level = AudioLevel.Silence)
            val vm = viewModel(recorder = recorder)

            vm.onEvent(HomeUiEvent.ActivateMic)
            advancePastInputWait()

            assertTrue(vm.uiState.value.micState is MicState.NoAudioRetry)
        }

    @Test
    fun given_consecutive_no_audio_then_retry_prompt_varies() =
        runTest(mainDispatcher.dispatcher) {
            val recorder = FakeAudioRecorder(level = AudioLevel.Silence)
            val vm = viewModel(recorder = recorder)

            vm.onEvent(HomeUiEvent.ActivateMic)
            advancePastInputWait()
            val first = (vm.uiState.value.micState as MicState.NoAudioRetry).promptIndex

            vm.onEvent(HomeUiEvent.RetryMic)
            advancePastInputWait()
            val second = (vm.uiState.value.micState as MicState.NoAudioRetry).promptIndex

            assertNotEquals(first, second)
        }

    @Test
    fun given_transcription_fails_when_stopped_then_error_state_with_category() =
        runTest(mainDispatcher.dispatcher) {
            val stt = FakeLiveTranscriptionRepository(finalFailure = SttException(SttErrorCategory.Network))
            val vm = viewModel(stt = stt)

            vm.onEvent(HomeUiEvent.ActivateMic)
            vm.onEvent(HomeUiEvent.StopRecording)
            runCurrent() // complete transcription but not the 30s error-idle timer

            assertEquals(MicState.Error(SttErrorCategory.Network), vm.uiState.value.micState)
        }

    @Test
    fun given_error_when_idle_timeout_elapses_then_returns_to_idle() =
        runTest(mainDispatcher.dispatcher) {
            val stt = FakeLiveTranscriptionRepository(finalFailure = SttException(SttErrorCategory.Generic))
            val vm = viewModel(stt = stt)

            vm.onEvent(HomeUiEvent.ActivateMic)
            vm.onEvent(HomeUiEvent.StopRecording)
            advanceUntilIdle()

            assertEquals(MicState.Idle, vm.uiState.value.micState)
        }

    @Test
    fun given_slow_transcription_then_shows_slow_response_then_success() =
        runTest(mainDispatcher.dispatcher) {
            // The final transcript takes longer than the 30s slow-response threshold (but within budget).
            val stt = FakeLiveTranscriptionRepository(finalText = "lámpara", finalDelayMillis = 40_000)
            val vm = viewModel(stt = stt)

            vm.onEvent(HomeUiEvent.ActivateMic)
            vm.onEvent(HomeUiEvent.StopRecording)
            advanceTimeBy(31_000)
            runCurrent()
            assertEquals(MicState.SlowResponse, vm.uiState.value.micState)

            advanceUntilIdle()
            assertEquals(MicState.Success("lámpara"), vm.uiState.value.micState)
        }

    @Test
    fun given_recording_ended_then_transcribing_keeps_the_heard_text() =
        runTest(mainDispatcher.dispatcher) {
            val stt = FakeLiveTranscriptionRepository(partials = listOf("busco un"), finalDelayMillis = 1_000)
            val vm = viewModel(stt = stt)

            vm.onEvent(HomeUiEvent.ActivateMic)
            vm.onEvent(HomeUiEvent.StopRecording)

            assertEquals(MicState.Transcribing("busco un"), vm.uiState.value.micState)
            vm.onEvent(HomeUiEvent.CancelMic)
        }

    @Test
    fun given_results_arrive_after_recording_ends_then_transcribing_shows_them() =
        runTest(mainDispatcher.dispatcher) {
            val stt = FakeLiveTranscriptionRepository(
                partials = emptyList(),
                latePartials = listOf("amola", "amoladora"),
                finalText = "amoladora",
                finalDelayMillis = 1_000,
            )
            val vm = viewModel(stt = stt)

            vm.onEvent(HomeUiEvent.ActivateMic)
            assertEquals("", (vm.uiState.value.micState as MicState.Listening).liveTranscript)
            vm.onEvent(HomeUiEvent.StopRecording)

            assertEquals(MicState.Transcribing("amoladora"), vm.uiState.value.micState)
            advanceUntilIdle()
            assertEquals(MicState.Success("amoladora"), vm.uiState.value.micState)
        }

    @Test
    fun given_voice_search_succeeded_then_controls_stay_hidden_during_navigation() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel(stt = FakeLiveTranscriptionRepository(finalText = "amoladora"))

            vm.onEvent(HomeUiEvent.ActivateMic)
            vm.onEvent(HomeUiEvent.StopRecording)
            advanceUntilIdle()

            assertEquals(MicState.Success("amoladora"), vm.uiState.value.micState)
            assertFalse(vm.uiState.value.areControlsVisible)
        }

    @Test
    fun given_voice_search_succeeded_when_screen_stopped_then_home_is_idle() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel(stt = FakeLiveTranscriptionRepository(finalText = "amoladora"))
            vm.onEvent(HomeUiEvent.ActivateMic)
            vm.onEvent(HomeUiEvent.StopRecording)
            advanceUntilIdle()

            vm.onEvent(HomeUiEvent.ScreenStopped)

            assertEquals(MicState.Idle, vm.uiState.value.micState)
            assertTrue(vm.uiState.value.areControlsVisible)
        }

    @Test
    fun given_error_screen_when_screen_stopped_then_error_is_kept() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel(stt = FakeLiveTranscriptionRepository(finalFailure = SttException(SttErrorCategory.Network)))
            vm.onEvent(HomeUiEvent.ActivateMic)
            vm.onEvent(HomeUiEvent.StopRecording)
            runCurrent()

            vm.onEvent(HomeUiEvent.ScreenStopped)

            assertEquals(MicState.Error(SttErrorCategory.Network), vm.uiState.value.micState)
        }

    @Test
    fun given_no_audio_prompt_when_30s_pass_then_home_is_idle() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel(recorder = FakeAudioRecorder(level = AudioLevel.Silence))
            vm.onEvent(HomeUiEvent.ActivateMic)
            advancePastInputWait()
            assertTrue(vm.uiState.value.micState is MicState.NoAudioRetry)

            // The prompt appeared up to 1 s before advancePastInputWait returned.
            advanceTimeBy(VoiceSearchConstants.ERROR_IDLE_TIMEOUT_SECONDS * 1_000L - 2_000)
            runCurrent()
            assertTrue(vm.uiState.value.micState is MicState.NoAudioRetry)

            advanceTimeBy(2_000)
            runCurrent()
            assertEquals(MicState.Idle, vm.uiState.value.micState)
        }

    @Test
    fun given_no_audio_prompt_when_retried_before_30s_then_it_keeps_listening() =
        runTest(mainDispatcher.dispatcher) {
            val recorder = FakeAudioRecorder(level = AudioLevel.Silence)
            val vm = viewModel(recorder = recorder)
            vm.onEvent(HomeUiEvent.ActivateMic)
            advancePastInputWait()

            advanceTimeBy(20_000)
            recorder.level = AudioLevel.Speech
            vm.onEvent(HomeUiEvent.RetryMic)
            advanceTimeBy(15_000) // past the first prompt's 30 s
            runCurrent()

            assertTrue(vm.uiState.value.micState is MicState.Listening)
        }

    @Test
    fun given_listening_when_screen_stopped_then_recorder_released_and_home_idle() =
        runTest(mainDispatcher.dispatcher) {
            val recorder = FakeAudioRecorder()
            val vm = viewModel(recorder = recorder)
            vm.onEvent(HomeUiEvent.ActivateMic)
            runCurrent()
            assertTrue(vm.uiState.value.micState is MicState.Listening)

            vm.onEvent(HomeUiEvent.ScreenStopped)
            runCurrent()

            assertEquals(MicState.Idle, vm.uiState.value.micState)
            assertFalse(recorder.isRecording)
        }

    @Test
    fun given_transcribing_when_screen_stopped_then_no_search_is_started() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel(stt = FakeLiveTranscriptionRepository(finalText = "amoladora", finalDelayMillis = 5_000))
            vm.uiEffect.test {
                vm.onEvent(HomeUiEvent.ActivateMic)
                vm.onEvent(HomeUiEvent.StopRecording)
                runCurrent()

                vm.onEvent(HomeUiEvent.ScreenStopped)
                advanceUntilIdle()

                assertEquals(MicState.Idle, vm.uiState.value.micState)
                expectNoEvents()
            }
        }

    @Test
    fun given_listening_when_cancelled_then_returns_to_idle_and_recorder_cancelled() =
        runTest(mainDispatcher.dispatcher) {
            val recorder = FakeAudioRecorder()
            val vm = viewModel(recorder = recorder)

            vm.onEvent(HomeUiEvent.ActivateMic)
            vm.onEvent(HomeUiEvent.CancelMic)

            assertEquals(MicState.Idle, vm.uiState.value.micState)
            assertTrue(vm.uiState.value.areControlsVisible)
            assertTrue(recorder.cancelled)
        }

    @Test
    fun given_open_catalog_when_event_then_navigation_effect_emitted() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()

            vm.uiEffect.test {
                vm.onEvent(HomeUiEvent.OpenCatalog)
                assertEquals(HomeUiEffect.NavigateToCatalog, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun given_query_when_submit_search_then_product_search_navigation_with_text_source() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()

            vm.uiEffect.test {
                vm.onEvent(HomeUiEvent.QueryChanged("lamps"))
                vm.onEvent(HomeUiEvent.SubmitSearch)
                assertEquals(
                    HomeUiEffect.NavigateToProductSearch("lamps", SearchSource.Text),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun given_blank_query_when_submit_search_then_no_navigation() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel()

            vm.uiEffect.test {
                vm.onEvent(HomeUiEvent.QueryChanged("   "))
                vm.onEvent(HomeUiEvent.SubmitSearch)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    /** Runs the silent listening window to its end, without reaching the 30 s idle timer after it. */
    private fun kotlinx.coroutines.test.TestScope.advancePastInputWait() {
        advanceTimeBy(VoiceSearchConstants.INPUT_WAIT_SECONDS * 1_000L + 1_000)
        runCurrent()
    }
}
