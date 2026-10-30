package com.qhathia.mobile.discovery.feature.pairing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.common.ElapsedRealtimeClock
import com.qhathia.mobile.discovery.core.domain.model.HeldPairingCode
import com.qhathia.mobile.discovery.core.domain.model.PairingCode
import com.qhathia.mobile.discovery.core.domain.model.PairingStatus
import com.qhathia.mobile.discovery.core.domain.usecase.ExchangeDeviceTokenUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GeneratePairingCodeUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GetHeldPairingCodeUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GetPairedDeviceUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.HoldPairingCodeUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObservePairingStatusUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.UnpairDeviceUseCase
import com.qhathia.mobile.discovery.feature.pairing.model.PairingError
import com.qhathia.mobile.discovery.feature.pairing.model.PairingPhase
import com.qhathia.mobile.discovery.feature.pairing.model.PairingUiEffect
import com.qhathia.mobile.discovery.feature.pairing.model.PairingUiEvent
import com.qhathia.mobile.discovery.feature.pairing.model.PairingUiState
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
 * device token is already persisted) or shows a code and waits for the merchant's approval over the
 * status channel. The code's TTL drives a one-second countdown that, on reaching zero, requests a
 * fresh code — mirroring the server's `EXPIRED` push. Unpairing is wired but unavailable server-side.
 *
 * The code survives the section being closed and recreated (design D6): it is held outside this
 * ViewModel ([HoldPairingCodeUseCase]), so [start] shows it again with its remaining time while it is
 * valid, also after "Volver" destroyed this ViewModel. Its expiry is measured on
 * [ElapsedRealtimeClock] so a screen off/on is counted. Socket drops are handled by the repository
 * (it reconnects), so they never show an error.
 *
 * The ViewModel is host-agnostic: it always drives pairing to completion and announces success via
 * [PairingUiEffect.Paired]; the host (Settings or onboarding) decides what success means.
 */
@HiltViewModel
class PairingViewModel @Inject constructor(
    private val observeStoreSession: ObserveStoreSessionUseCase,
    private val generatePairingCode: GeneratePairingCodeUseCase,
    private val observePairingStatus: ObservePairingStatusUseCase,
    private val exchangeDeviceToken: ExchangeDeviceTokenUseCase,
    private val getPairedDevice: GetPairedDeviceUseCase,
    private val unpairDevice: UnpairDeviceUseCase,
    private val getHeldPairingCode: GetHeldPairingCodeUseCase,
    private val holdPairingCode: HoldPairingCodeUseCase,
    private val clock: ElapsedRealtimeClock,
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
    private var exchangeJob: Job? = null
    private var generateJob: Job? = null

    /** Set when [start] finds a paired session: this section then never requests a pairing code. */
    private var pairedSession = false

    /**
     * Enter the pairing section: load the paired device when already paired, resume the held code
     * when it is still valid, otherwise generate one. Called from the screen's composition (see
     * [stop]), so a code is only generated while the merchant is actually viewing the section.
     */
    fun start() {
        viewModelScope.launch {
            when {
                observeStoreSession().first().isPaired -> {
                    pairedSession = true
                    loadPairedDevice()
                }
                exchangeJob?.isActive == true -> Unit // pairing is completing
                else -> resumeOrStartPairing()
            }
        }
    }

    /** Leave the pairing section: tear down the status subscription, countdown and code request. */
    fun stop() {
        cancelJobs()
        generateJob?.cancel()
    }

    fun onEvent(event: PairingUiEvent) {
        when (event) {
            PairingUiEvent.GenerateCode -> onGenerateCode()
            PairingUiEvent.RetryPairedDeviceLoad -> if (pairedSession) loadPairedDevice()
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
                .onFailure { showError(PairingError.PairedDeviceLoad) }
        }
    }

    /**
     * "Generar nuevo código" / "Reintentar" (wave 2, D8). Fast taps are dropped while a code request is
     * running (VIN-024), and a paired tablet never requests a code from this section (AJU-050).
     */
    private fun onGenerateCode() {
        if (pairedSession || generateJob?.isActive == true) return
        startPairing()
    }

    private fun resumeOrStartPairing() {
        val held = getHeldPairingCode()
        val remaining = held?.let(::remainingSeconds) ?: 0L
        if (held == null || remaining <= 0) {
            startPairing()
            return
        }
        _uiState.update {
            it.copy(
                phase = PairingPhase.WaitingForApproval,
                code = held.code.code,
                remainingSeconds = remaining,
                totalSeconds = held.code.ttlSeconds,
                error = null,
            )
        }
        startCountdown()
        observeStatus(held.code.sessionId)
    }

    private fun startPairing() {
        cancelJobs()
        holdPairingCode(null)
        _uiState.update {
            it.copy(phase = PairingPhase.Loading, code = null, remainingSeconds = 0L, error = null)
        }
        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            generatePairingCode()
                .onSuccess(::onCodeGenerated)
                .onFailure { showError(PairingError.CodeGeneration) }
        }
    }

    private fun onCodeGenerated(code: PairingCode) {
        // Seed both the countdown and the progress bar from the server-granted TTL (which may differ
        // from the documented 5 min). Using the relative TTL instead of expiresAt − now keeps the timer
        // immune to device↔server clock skew; the expiry is then tracked on the monotonic clock.
        val ttl = code.ttlSeconds
        holdPairingCode(
            HeldPairingCode(code, expiresAtElapsedMillis = clock.elapsedRealtimeMillis() + ttl * ONE_SECOND_MS),
        )
        _uiState.update {
            it.copy(
                phase = PairingPhase.WaitingForApproval,
                code = code.code,
                remainingSeconds = ttl,
                totalSeconds = ttl,
            )
        }
        startCountdown()
        observeStatus(code.sessionId)
    }

    private fun remainingSeconds(code: HeldPairingCode): Long {
        val leftMillis = code.expiresAtElapsedMillis - clock.elapsedRealtimeMillis()
        return ((leftMillis + ONE_SECOND_MS - 1) / ONE_SECOND_MS).coerceAtLeast(0)
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = getHeldPairingCode()?.let(::remainingSeconds) ?: 0L
            while (remaining > 0) {
                delay(ONE_SECOND_MS)
                remaining = getHeldPairingCode()?.let(::remainingSeconds) ?: 0L
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
                // The repository reconnects dropped sockets itself; it fails only when the session
                // can no longer be paired (e.g. its token was already used).
                .catch { showError(PairingError.Linking) }
                .collect { status ->
                    when (status) {
                        PairingStatus.Waiting -> Unit
                        PairingStatus.Approved -> onApproved(sessionId)
                        PairingStatus.Rejected -> {
                            cancelJobs()
                            holdPairingCode(null)
                            _uiState.update { it.copy(phase = PairingPhase.Rejected) }
                        }
                        PairingStatus.Expired -> startPairing()
                    }
                }
        }
    }

    private fun onApproved(sessionId: String) {
        if (exchangeJob?.isActive == true) return
        countdownJob?.cancel()
        _uiState.update { it.copy(phase = PairingPhase.Loading) }
        exchangeJob = viewModelScope.launch {
            // The repository exchanges at most once per session, so an APPROVED seen again after a
            // reconnect gets the same device without a second request.
            exchangeDeviceToken(sessionId)
                .onSuccess { device ->
                    statusJob?.cancel()
                    holdPairingCode(null)
                    _uiState.update { it.copy(phase = PairingPhase.Paired, device = device) }
                    // Pairing completed and the device token was persisted: announce success so the
                    // host can react (Settings shows the details in place; onboarding navigates Home).
                    _uiEffect.emit(PairingUiEffect.Paired)
                }
                .onFailure { showError(PairingError.Linking) }
        }
    }

    private fun showError(error: PairingError) {
        cancelJobs()
        holdPairingCode(null)
        _uiState.update { it.copy(phase = PairingPhase.Error, error = error) }
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
