package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.SettingsConstants
import com.qhathia.mobile.discovery.core.domain.model.PinLockState
import com.qhathia.mobile.discovery.core.domain.usecase.IsSettingsPinRequiredUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObservePinLockUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RegisterPinFailureUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ResetPinAttemptsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateSettingsPasswordUseCase
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEffect
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEvent
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
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
 * Drives the settings PIN gate. On [SettingsUiEvent.Reset] (every time the gate opens) it checks
 * whether an exit PIN is set: if not, it emits [SettingsUiEffect.Unlocked] straight away and the
 * dialog never shows. Otherwise the keypad accepts 4–6 digits: the confirm key validates from 4, and
 * the 6th digit validates automatically. A correct PIN emits [SettingsUiEffect.Unlocked]; a wrong one
 * clears the entry and shows an inline error.
 *
 * **Lockout (design D9, AJU-011).** The 5th wrong PIN in a row locks the gate for 24 hours: the keypad
 * is disabled and the time left is shown, refreshed every minute. The lock is persisted, so it holds
 * across closing the dialog and restarts. A correct PIN resets the count; a check that could not run
 * (an error, not a wrong PIN) is not counted.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val validateSettingsPassword: ValidateSettingsPasswordUseCase,
    private val isSettingsPinRequired: IsSettingsPinRequiredUseCase,
    private val observePinLock: ObservePinLockUseCase,
    private val registerPinFailure: RegisterPinFailureUseCase,
    private val resetPinAttempts: ResetPinAttemptsUseCase,
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

    /** Keeps the lock and its time left on screen while the gate is open. */
    private fun followLock() {
        lockJob?.cancel()
        lockJob = viewModelScope.launch {
            observePinLock().collect { lock -> _uiState.update { it.copy(lockedMinutesLeft = lock.minutesLeft()) } }
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
                        val lock = registerPinFailure()
                        // Clear the entry so the merchant can retype the PIN (unless now locked).
                        _uiState.update {
                            it.copy(
                                passwordInput = "",
                                showError = lock is PinLockState.Open,
                                isValidating = false,
                                lockedMinutesLeft = lock.minutesLeft(),
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

    private fun leave(effect: SettingsUiEffect) {
        lockJob?.cancel()
        emit(effect)
    }

    private fun emit(effect: SettingsUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    /** Minutes left on a lock, rounded up so "00:00" is never shown while still locked. */
    private fun PinLockState.minutesLeft(): Long? = when (this) {
        is PinLockState.Open -> null
        is PinLockState.Locked -> {
            val minutes = (remaining.inWholeSeconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE
            minutes.coerceAtLeast(1)
        }
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60L
    }
}
