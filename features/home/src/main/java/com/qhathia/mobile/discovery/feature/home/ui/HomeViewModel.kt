package com.qhathia.mobile.discovery.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.VoiceSearchConstants
import com.qhathia.mobile.discovery.core.domain.audio.AudioLevel
import com.qhathia.mobile.discovery.core.domain.audio.AudioRecorder
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.TranscribeVoiceSearchUseCase
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
 * `Idle → Listening(record) → Transcribing → (SlowResponse) → Success | Error`, plus `NoAudioRetry`
 * when nothing was captured. Recording is bounded by the input-wait (30s) and max-recording (45s)
 * caps; a slow transcript shows the slow-response message after 30s; an error screen auto-returns to
 * idle after 30s. The use case enforces the 150s total-operation budget.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeStoreSession: ObserveStoreSessionUseCase,
    private val audioRecorder: AudioRecorder,
    private val transcribeVoiceSearch: TranscribeVoiceSearchUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<HomeUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<HomeUiEffect> = _uiEffect.asSharedFlow()

    private var listeningJob: Job? = null
    private var transcribeJob: Job? = null
    private var errorIdleJob: Job? = null

    /** Guards against the listening countdown and a user stop both starting transcription. */
    private var finishing = false

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
            HomeUiEvent.StopRecording -> beginTranscription()
            HomeUiEvent.CancelMic -> cancelVoiceSearch()
            is HomeUiEvent.QueryChanged -> _uiState.update { it.copy(query = event.query) }
            HomeUiEvent.SubmitSearch -> {
                val query = _uiState.value.query.trim()
                if (query.isNotEmpty()) {
                    emitEffect(HomeUiEffect.NavigateToProductSearch(query, SearchSource.Text))
                }
            }
            HomeUiEvent.OpenCatalog -> emitEffect(HomeUiEffect.NavigateToCatalog)
            HomeUiEvent.OpenSettings -> emitEffect(HomeUiEffect.NavigateToSettings)
        }
    }

    private fun startVoiceSearch() {
        cancelAllJobs()
        finishing = false
        listeningJob = viewModelScope.launch {
            audioRecorder.start()
            _uiState.update { it.copy(micState = MicState.Listening(VoiceSearchConstants.MAX_RECORDING_SECONDS)) }

            val pollMs = VoiceSearchConstants.LEVEL_POLL_INTERVAL_MS
            val inputWaitMs = VoiceSearchConstants.INPUT_WAIT_SECONDS * ONE_SECOND_MS
            val maxRecordingMs = VoiceSearchConstants.MAX_RECORDING_SECONDS * ONE_SECOND_MS

            var elapsedMs = 0L
            var hasSpoken = false
            var trailingSilenceMs = 0L

            while (true) {
                delay(pollMs)
                elapsedMs += pollMs
                when (audioRecorder.pollLevel()) {
                    AudioLevel.Speech -> {
                        hasSpoken = true
                        trailingSilenceMs = 0
                    }
                    AudioLevel.Silence -> if (hasSpoken) trailingSilenceMs += pollMs
                }
                updateListeningRemaining(maxRecordingMs - elapsedMs)

                // End-of-speech: once the consumer has spoken, a short trailing silence ends the
                // recording and triggers transcription immediately.
                if (hasSpoken && trailingSilenceMs >= VoiceSearchConstants.SILENCE_AUTO_STOP_MS) {
                    beginTranscription()
                    return@launch
                }
                // No-audio: the input-wait window elapsed without any detected speech.
                if (!hasSpoken && elapsedMs >= inputWaitMs) {
                    beginNoAudio()
                    return@launch
                }
                // Hard cap on the recording length.
                if (elapsedMs >= maxRecordingMs) {
                    beginTranscription()
                    return@launch
                }
            }
        }
    }

    private fun updateListeningRemaining(remainingMs: Long) {
        val remaining = (remainingMs / ONE_SECOND_MS).toInt().coerceAtLeast(0)
        _uiState.update { state ->
            if (state.micState is MicState.Listening) {
                state.copy(micState = MicState.Listening(remaining))
            } else {
                state
            }
        }
    }

    /** Stops recording and starts transcription. Safe to call from the listening loop or a user stop. */
    private fun beginTranscription() {
        if (finishing) return
        if (_uiState.value.micState !is MicState.Listening) return
        finishing = true
        listeningJob?.cancel()
        listeningJob = null
        transcribeJob = viewModelScope.launch {
            val audio = audioRecorder.stop()
            if (audio == null) {
                finishing = false
                showNoAudio()
                return@launch
            }
            _uiState.update { it.copy(micState = MicState.Transcribing) }
            val slowResponseJob = launch {
                delay(VoiceSearchConstants.SLOW_RESPONSE_SECONDS * ONE_SECOND_MS)
                _uiState.update { state ->
                    if (state.micState is MicState.Transcribing) {
                        state.copy(micState = MicState.SlowResponse)
                    } else {
                        state
                    }
                }
            }
            val result = transcribeVoiceSearch(audio)
            slowResponseJob.cancel()
            finishing = false
            result.fold(
                onSuccess = { onTranscribed(it.transcript) },
                onFailure = { onTranscriptionError(it) },
            )
        }
    }

    private fun beginNoAudio() {
        if (finishing) return
        finishing = true
        transcribeJob = viewModelScope.launch {
            audioRecorder.stop()
            finishing = false
            showNoAudio()
        }
    }

    private fun onTranscribed(transcript: String) {
        _uiState.update { it.copy(micState = MicState.Success(transcript)) }
        emitEffect(HomeUiEffect.NavigateToProductSearch(transcript, SearchSource.Voice))
    }

    private fun onTranscriptionError(error: Throwable) {
        val category = (error as? SttException)?.category ?: SttErrorCategory.Generic
        _uiState.update { it.copy(micState = MicState.Error(category)) }
        errorIdleJob = viewModelScope.launch {
            delay(VoiceSearchConstants.ERROR_IDLE_TIMEOUT_SECONDS * ONE_SECOND_MS)
            _uiState.update { state ->
                if (state.micState is MicState.Error) state.copy(micState = MicState.Idle) else state
            }
        }
    }

    private fun showNoAudio() {
        val index = retryCount++
        _uiState.update { it.copy(micState = MicState.NoAudioRetry(promptIndex = index)) }
    }

    private fun cancelVoiceSearch() {
        cancelAllJobs()
        finishing = false
        audioRecorder.cancel()
        _uiState.update { it.copy(micState = MicState.Idle) }
    }

    private fun cancelAllJobs() {
        listeningJob?.cancel()
        listeningJob = null
        transcribeJob?.cancel()
        transcribeJob = null
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
