package com.qhathia.mobile.discovery.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.VoiceSearchConstants
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.VoiceSessionEvent
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RunVoiceSearchSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.VoiceSearchSession
import com.qhathia.mobile.discovery.feature.home.model.HomeUiEffect
import com.qhathia.mobile.discovery.feature.home.model.HomeUiEvent
import com.qhathia.mobile.discovery.feature.home.model.HomeUiState
import com.qhathia.mobile.discovery.feature.home.model.MicState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Discovery Home screen and owns the voice-search state machine:
 *
 * `Idle → Listening(live transcript) → Transcribing → (SlowResponse) → Success | Error`, plus
 * `NoAudioRetry` when no speech was detected. Recording, endpointing (30s input wait, 45s cap, 1s
 * trailing silence) and live transcription run in [RunVoiceSearchSessionUseCase]; this ViewModel maps
 * its events to [MicState]. A slow final transcript shows the slow-response message after 30s; an
 * error screen auto-returns to idle after 30s.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeStoreSession: ObserveStoreSessionUseCase,
    private val runVoiceSearchSession: RunVoiceSearchSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<HomeUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<HomeUiEffect> = _uiEffect.asSharedFlow()

    private var voiceSession: VoiceSearchSession? = null
    private var voiceJob: Job? = null
    private var slowResponseJob: Job? = null
    private var errorIdleJob: Job? = null

    /** Monotonic counter used to pick and vary the retry prompt across consecutive no-audio outcomes. */
    private var retryCount = 0

    init {
        viewModelScope.launch {
            observeStoreSession().collect { session ->
                _uiState.update { it.copy(storeName = session.storeName) }
            }
        }
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.ActivateMic -> startVoiceSearch()
            HomeUiEvent.RetryMic -> startVoiceSearch()
            HomeUiEvent.StopRecording -> stopRecording()
            HomeUiEvent.CancelMic -> cancelVoiceSearch()
            is HomeUiEvent.QueryChanged -> _uiState.update { it.copy(query = event.query) }
            HomeUiEvent.SubmitSearch -> {
                val query = _uiState.value.query.trim()
                if (query.isNotEmpty()) {
                    // Clear the field so the search bar is empty when the user returns to Home.
                    _uiState.update { it.copy(query = "") }
                    emitEffect(HomeUiEffect.NavigateToProductSearch(query, SearchSource.Text))
                }
            }
            HomeUiEvent.OpenCatalog -> emitEffect(HomeUiEffect.NavigateToCatalog)
            HomeUiEvent.OpenSettings -> emitEffect(HomeUiEffect.NavigateToSettings)
            HomeUiEvent.ScreenStopped -> onScreenStopped()
        }
    }

    /**
     * Home is hidden (another screen on top, or the app in the background). A finished voice search
     * stays on screen during the navigation transition; reset it now so Home is idle when the consumer
     * comes back. A voice search still in progress is cancelled, which releases the microphone
     * (wave 2, D3: VOZ-004).
     */
    private fun onScreenStopped() {
        when (_uiState.value.micState) {
            is MicState.Listening, is MicState.Transcribing, MicState.SlowResponse -> cancelVoiceSearch()
            is MicState.Success -> _uiState.update { it.copy(micState = MicState.Idle) }
            else -> Unit
        }
    }

    private fun startVoiceSearch() {
        val previous = voiceJob
        cancelAllJobs()
        val session = runVoiceSearchSession()
        voiceSession = session
        voiceJob = viewModelScope.launch {
            // The recorder is shared: let a cancelled session release it before this one starts.
            previous?.join()
            session.events.collect(::onVoiceEvent)
        }
    }

    private fun onVoiceEvent(event: VoiceSessionEvent) {
        when (event) {
            is VoiceSessionEvent.Listening -> _uiState.update {
                it.copy(micState = MicState.Listening(event.remainingSeconds, event.transcript))
            }
            is VoiceSessionEvent.Finalizing -> onFinalizing(event.transcript)
            is VoiceSessionEvent.Transcribed -> {
                cancelSlowResponseTimer()
                onTranscribed(event.transcript)
            }
            VoiceSessionEvent.NoAudio -> showNoAudio()
            is VoiceSessionEvent.Failed -> {
                cancelSlowResponseTimer()
                onTranscriptionError(event.category)
            }
        }
    }

    /**
     * Recording ended. The first event switches to [MicState.Transcribing] and starts the slow-response
     * timer; later ones only refresh the heard text (results keep arriving after the recording ends).
     * Once the slow-response message is up it stays.
     */
    private fun onFinalizing(transcript: String) {
        when (_uiState.value.micState) {
            is MicState.Listening -> {
                _uiState.update { it.copy(micState = MicState.Transcribing(transcript)) }
                startSlowResponseTimer()
            }
            is MicState.Transcribing -> _uiState.update { it.copy(micState = MicState.Transcribing(transcript)) }
            else -> Unit
        }
    }

    /** The consumer tapped the mic while recording: end the recording now and await the final transcript. */
    private fun stopRecording() {
        if (_uiState.value.micState is MicState.Listening) voiceSession?.stop()
    }

    private fun startSlowResponseTimer() {
        cancelSlowResponseTimer()
        slowResponseJob = viewModelScope.launch {
            delay(VoiceSearchConstants.SLOW_RESPONSE_SECONDS * ONE_SECOND_MS)
            _uiState.update { state ->
                if (state.micState is MicState.Transcribing) state.copy(micState = MicState.SlowResponse) else state
            }
        }
    }

    private fun cancelSlowResponseTimer() {
        slowResponseJob?.cancel()
        slowResponseJob = null
    }

    private fun onTranscribed(transcript: String) {
        // Clear any leftover typed query so the field is empty when the user returns to Home.
        _uiState.update { it.copy(micState = MicState.Success(transcript), query = "") }
        emitEffect(HomeUiEffect.NavigateToProductSearch(transcript, SearchSource.Voice))
    }

    private fun onTranscriptionError(category: SttErrorCategory) {
        _uiState.update { it.copy(micState = MicState.Error(category)) }
        startErrorIdleTimer()
    }

    private fun showNoAudio() {
        val index = retryCount++
        _uiState.update { it.copy(micState = MicState.NoAudioRetry(promptIndex = index)) }
        // "No te escuché…" goes back to idle like the error screens (wave 2, D3: INI-051, VOZ-003).
        startErrorIdleTimer()
    }

    /** Returns an error or no-audio screen to idle after [VoiceSearchConstants.ERROR_IDLE_TIMEOUT_SECONDS]. */
    private fun startErrorIdleTimer() {
        errorIdleJob?.cancel()
        errorIdleJob = viewModelScope.launch {
            delay(VoiceSearchConstants.ERROR_IDLE_TIMEOUT_SECONDS * ONE_SECOND_MS)
            _uiState.update { state ->
                when (state.micState) {
                    is MicState.Error, is MicState.NoAudioRetry -> state.copy(micState = MicState.Idle)
                    else -> state
                }
            }
        }
    }

    private fun cancelVoiceSearch() {
        // Cancelling the session's collection releases the microphone and closes the live stream.
        cancelAllJobs()
        _uiState.update { it.copy(micState = MicState.Idle) }
    }

    private fun cancelAllJobs() {
        voiceJob?.cancel()
        voiceJob = null
        voiceSession = null
        cancelSlowResponseTimer()
        errorIdleJob?.cancel()
        errorIdleJob = null
    }

    private fun emitEffect(effect: HomeUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    private companion object {
        const val ONE_SECOND_MS = 1_000L
    }
}
