package com.qhathia.mobile.discovery.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.HomeConstants
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
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
 * Drives the Discovery Home screen: surfaces the store name from the session and owns the
 * microphone interaction state machine (idle → listening with a 30s cap → no-audio retry).
 *
 * Real speech-to-text is out of scope; "no audio detected" is modelled as the listening window
 * elapsing. The Deepgram integration will later replace the timeout-only path.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeStoreSession: ObserveStoreSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<HomeUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<HomeUiEffect> = _uiEffect.asSharedFlow()

    private var listeningJob: Job? = null

    /** Monotonic counter used to pick and vary the retry prompt across consecutive no-audio timeouts. */
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
            HomeUiEvent.ActivateMic -> startListening()
            HomeUiEvent.RetryMic -> startListening()
            HomeUiEvent.CancelMic -> stopListening()
            is HomeUiEvent.QueryChanged -> _uiState.update { it.copy(query = event.query) }
            HomeUiEvent.SubmitSearch -> emitEffect(HomeUiEffect.NavigateToResults(_uiState.value.query))
            HomeUiEvent.OpenCatalog -> emitEffect(HomeUiEffect.NavigateToCatalog)
            HomeUiEvent.OpenSettings -> emitEffect(HomeUiEffect.NavigateToSettings)
        }
    }

    private fun startListening() {
        listeningJob?.cancel()
        val total = HomeConstants.MIC_LISTENING_TIMEOUT_SECONDS
        _uiState.update { it.copy(micState = MicState.Listening(remainingSeconds = total)) }
        listeningJob = viewModelScope.launch {
            for (remaining in total - 1 downTo 0) {
                delay(ONE_SECOND_MS)
                _uiState.update { state ->
                    if (state.micState is MicState.Listening) {
                        state.copy(micState = MicState.Listening(remainingSeconds = remaining))
                    } else {
                        state
                    }
                }
            }
            onNoAudioTimeout()
        }
    }

    private fun onNoAudioTimeout() {
        val index = retryCount++
        _uiState.update { it.copy(micState = MicState.NoAudioRetry(promptIndex = index)) }
    }

    private fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
        _uiState.update { it.copy(micState = MicState.Idle) }
    }

    private fun emitEffect(effect: HomeUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    private companion object {
        const val ONE_SECOND_MS = 1_000L
    }
}
