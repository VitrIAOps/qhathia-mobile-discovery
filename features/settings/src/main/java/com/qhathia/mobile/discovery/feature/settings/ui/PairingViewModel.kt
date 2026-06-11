package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.model.PairingCode
import com.qhathia.mobile.discovery.core.domain.model.PairingStatus
import com.qhathia.mobile.discovery.core.domain.usecase.ExchangeDeviceTokenUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GeneratePairingCodeUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GetPairedDeviceUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObservePairingStatusUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.UnpairDeviceUseCase
import com.qhathia.mobile.discovery.feature.settings.model.PairingPhase
import com.qhathia.mobile.discovery.feature.settings.model.PairingUiEffect
import com.qhathia.mobile.discovery.feature.settings.model.PairingUiEvent
import com.qhathia.mobile.discovery.feature.settings.model.PairingUiState
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the pairing (Vincular) section. On entry it either loads the paired-device details (when a
 * device token is already persisted) or generates a code and waits for the merchant's approval over
 * the status channel. The code's TTL drives a one-second countdown that, on reaching zero, requests a
 * fresh code — mirroring the server's `EXPIRED` push. Unpairing is wired but unavailable server-side.
 */
@HiltViewModel
class PairingViewModel @Inject constructor(
    private val observeStoreSession: ObserveStoreSessionUseCase,
    private val generatePairingCode: GeneratePairingCodeUseCase,
    private val observePairingStatus: ObservePairingStatusUseCase,
    private val exchangeDeviceToken: ExchangeDeviceTokenUseCase,
    private val getPairedDevice: GetPairedDeviceUseCase,
    private val unpairDevice: UnpairDeviceUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PairingUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<PairingUiEffect> = _uiEffect.asSharedFlow()

    private var countdownJob: Job? = null
    private var statusJob: Job? = null

    /**
     * Enter the pairing section: load the paired device when already paired, otherwise generate a
     * code and begin waiting for approval. Called from the screen's composition (see [stop]), so a
     * code is only generated while the merchant is actually viewing the section.
     */
    fun start() {
        viewModelScope.launch {
            if (observeStoreSession().first().isPaired) loadPairedDevice() else startPairing()
        }
    }

    /** Leave the pairing section: tear down the status subscription and countdown. */
    fun stop() {
        cancelJobs()
    }

    fun onEvent(event: PairingUiEvent) {
        when (event) {
            PairingUiEvent.GenerateCode -> startPairing()
            PairingUiEvent.UnpairClicked ->
                _uiState.update { it.copy(showUnpairConfirmation = true) }
            PairingUiEvent.UnpairDismissed ->
                _uiState.update { it.copy(showUnpairConfirmation = false) }
            PairingUiEvent.UnpairConfirmed -> confirmUnpair()
        }
    }

    private fun loadPairedDevice() {
        _uiState.update { it.copy(phase = PairingPhase.Loading) }
        viewModelScope.launch {
            getPairedDevice()
                .onSuccess { device ->
                    _uiState.update { it.copy(phase = PairingPhase.Paired, device = device) }
                }
                .onFailure { _uiState.update { it.copy(phase = PairingPhase.Error) } }
        }
    }

    private fun startPairing() {
        cancelJobs()
        _uiState.update {
            it.copy(phase = PairingPhase.Loading, code = null, remainingSeconds = 0L)
        }
        viewModelScope.launch {
            generatePairingCode()
                .onSuccess(::onCodeGenerated)
                .onFailure { _uiState.update { it.copy(phase = PairingPhase.Error) } }
        }
    }

    private fun onCodeGenerated(code: PairingCode) {
        // Seed both the countdown and the progress bar from the server-granted TTL (which may differ
        // from the documented 5 min). Using the relative TTL instead of expiresAt − now keeps the timer
        // immune to device↔server clock skew; the per-second tick is already monotonic.
        val initialRemaining = code.ttlSeconds
        _uiState.update {
            it.copy(
                phase = PairingPhase.WaitingForApproval,
                code = code.code,
                remainingSeconds = initialRemaining,
                totalSeconds = initialRemaining,
            )
        }
        startCountdown(initialRemaining)
        observeStatus(code.sessionId)
    }

    private fun startCountdown(initialRemaining: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = initialRemaining
            while (remaining > 0) {
                delay(ONE_SECOND_MS)
                remaining--
                _uiState.update { it.copy(remainingSeconds = remaining) }
            }
            // Expired on screen: request a fresh code + session.
            startPairing()
        }
    }

    private fun observeStatus(sessionId: String) {
        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            observePairingStatus(sessionId)
                // A failed handshake (e.g. a rejected/expired session, ADR-009) or a dropped socket
                // surfaces here as a thrown exception; map it to the retryable Error phase instead of
                // letting it escape the coroutine and crash the process.
                .catch { onStatusStreamFailed() }
                .collect { status ->
                    when (status) {
                        PairingStatus.Waiting -> Unit
                        PairingStatus.Approved -> onApproved(sessionId)
                        PairingStatus.Rejected -> {
                            countdownJob?.cancel()
                            _uiState.update { it.copy(phase = PairingPhase.Rejected) }
                        }
                        PairingStatus.Expired -> startPairing()
                    }
                }
        }
    }

    private fun onStatusStreamFailed() {
        countdownJob?.cancel()
        _uiState.update { it.copy(phase = PairingPhase.Error) }
    }

    private fun onApproved(sessionId: String) {
        countdownJob?.cancel()
        _uiState.update { it.copy(phase = PairingPhase.Loading) }
        viewModelScope.launch {
            exchangeDeviceToken(sessionId)
                .onSuccess { device ->
                    _uiState.update { it.copy(phase = PairingPhase.Paired, device = device) }
                }
                .onFailure { _uiState.update { it.copy(phase = PairingPhase.Error) } }
        }
    }

    private fun confirmUnpair() {
        _uiState.update { it.copy(showUnpairConfirmation = false) }
        viewModelScope.launch {
            unpairDevice().onFailure { _uiEffect.emit(PairingUiEffect.UnpairUnavailable) }
        }
    }

    private fun cancelJobs() {
        countdownJob?.cancel()
        statusJob?.cancel()
    }

    override fun onCleared() {
        cancelJobs()
        super.onCleared()
    }

    private companion object {
        const val ONE_SECOND_MS = 1_000L
    }
}
