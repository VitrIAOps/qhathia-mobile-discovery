package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.SettingsConstants
import com.qhathia.mobile.discovery.core.domain.model.PinLockState
import com.qhathia.mobile.discovery.core.domain.usecase.IsSettingsPinRequiredUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObservePinLockUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveSettingsPinRequiredUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RegisterPinFailureUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ReportPinLockoutUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ResetPinAttemptsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateSettingsPasswordUseCase
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEffect
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEvent
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the settings PIN gate. On [SettingsUiEvent.Reset] (every time the gate opens) it checks
 * whether an exit PIN is set: if not, it emits [SettingsUiEffect.Unlocked] straight away and the
 * dialog never shows. Otherwise the keypad accepts 4–6 digits: the confirm key validates from 4, and
 * the 6th digit validates automatically. A correct PIN emits [SettingsUiEffect.Unlocked]; a wrong one
 * clears the entry and shows an inline error.
 *
 * **Lockout (design D9, settings-pin-lockout).** The 5th wrong PIN in a row locks the gate: 30 s the
 * first time, then 1, 2, 4, 8 and 15 min. The keypad is disabled and the time left is shown as m:ss,
 * refreshed every second. The lock is persisted, so it holds across closing the dialog and restarts.
 * Each new lock is reported to the backend once, fire-and-forget. A correct PIN resets the count and
 * the level; a check that could not run (an error, not a wrong PIN) is not counted. If the staff app
 * removes the PIN while the dialog is open, the gate opens.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val validateSettingsPassword: ValidateSettingsPasswordUseCase,
    private val isSettingsPinRequired: IsSettingsPinRequiredUseCase,
    private val observePinLock: ObservePinLockUseCase,
    private val registerPinFailure: RegisterPinFailureUseCase,
    private val resetPinAttempts: ResetPinAttemptsUseCase,
    private val observePinRequired: ObserveSettingsPinRequiredUseCase,
    private val reportPinLockout: ReportPinLockoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SettingsUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<SettingsUiEffect> = _uiEffect.asSharedFlow()

    private var accessJob: Job? = null
    private var lockJob: Job? = null

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.PasswordChanged -> onPasswordChanged(event.value)
            SettingsUiEvent.SubmitPassword -> submitPassword()
            SettingsUiEvent.DismissDialog -> leave(SettingsUiEffect.NavigateBack)
            SettingsUiEvent.Reset -> resolveAccess()
        }
    }

    private fun resolveAccess() {
        _uiState.value = SettingsUiState(isResolvingAccess = true)
        accessJob?.cancel()
        lockJob?.cancel()
        accessJob = viewModelScope.launch {
            if (isSettingsPinRequired()) {
                _uiState.update { it.copy(isResolvingAccess = false) }
                followLock()
            } else {
                // No exit PIN set from the staff app: the settings open without asking.
                emit(SettingsUiEffect.Unlocked)
            }
        }
    }

    /**
     * Keeps the lock and its time left on screen while the gate is open, and opens the gate if the
     * staff app removes the PIN meanwhile (settings-pin-lockout D5).
     */
    private fun followLock() {
        lockJob?.cancel()
        lockJob = viewModelScope.launch {
            launch {
                observePinLock().collect { lock -> _uiState.update { it.copy(lockedSecondsLeft = lock.secondsLeft()) } }
            }
            launch {
                observePinRequired().first { required -> !required }
                leave(SettingsUiEffect.Unlocked)
            }
        }
    }

    private fun onPasswordChanged(value: String) {
        if (!_uiState.value.isInputEnabled) return
        // The PIN pad only enters digits; cap at the longest valid PIN.
        val pin = value.filter { it.isDigit() }.take(SettingsConstants.MAX_PIN_LENGTH)
        _uiState.update { it.copy(passwordInput = pin, showError = false) }
        if (pin.length == SettingsConstants.MAX_PIN_LENGTH) submitPassword()
    }

    private fun submitPassword() {
        val state = _uiState.value
        if (!state.isInputEnabled || state.passwordInput.length < SettingsConstants.MIN_PIN_LENGTH) return
        _uiState.update { it.copy(isValidating = true) }
        viewModelScope.launch {
            validateSettingsPassword(state.passwordInput)
                .onSuccess { accepted ->
                    if (accepted) {
                        resetPinAttempts()
                        _uiState.update { it.copy(isValidating = false) }
                        leave(SettingsUiEffect.Unlocked)
                    } else {
                        val result = registerPinFailure()
                        val lock = result.state
                        // Decided by the domain: the lock observer may already show this lock (F-13).
                        if (result.startedLock && lock is PinLockState.Locked) reportLockout(lock)
                        // Clear the entry so the merchant can retype the PIN (unless now locked).
                        _uiState.update {
                            it.copy(
                                passwordInput = "",
                                showError = lock is PinLockState.Open,
                                isValidating = false,
                                lockedSecondsLeft = lock.secondsLeft(),
                            )
                        }
                    }
                }
                .onFailure {
                    // The PIN could not be checked: not a wrong PIN, so it is not counted.
                    _uiState.update { it.copy(passwordInput = "", showError = true, isValidating = false) }
                }
        }
    }

    /**
     * Tells the backend about a new lock, in its own coroutine; a failure changes nothing on screen. Not
     * cancelled if the PIN screen closes right after the lock (F-13); the HTTP timeouts bound it.
     */
    private fun reportLockout(lock: PinLockState.Locked) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                reportPinLockout(failedAttempts = SettingsConstants.PIN_MAX_ATTEMPTS, lockDuration = lock.remaining)
            }
        }
    }

    private fun leave(effect: SettingsUiEffect) {
        lockJob?.cancel()
        emit(effect)
    }

    private fun emit(effect: SettingsUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    /** Seconds left on a lock, rounded up so "0:00" is never shown while still locked. */
    private fun PinLockState.secondsLeft(): Long? = when (this) {
        is PinLockState.Open -> null
        is PinLockState.Locked -> {
            val millis = remaining.inWholeMilliseconds
            ((millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).coerceAtLeast(1)
        }
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
