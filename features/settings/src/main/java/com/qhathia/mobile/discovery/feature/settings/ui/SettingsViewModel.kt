package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.SettingsConstants
import com.qhathia.mobile.discovery.core.domain.usecase.IsSettingsPinRequiredUseCase
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
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val validateSettingsPassword: ValidateSettingsPasswordUseCase,
    private val isSettingsPinRequired: IsSettingsPinRequiredUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SettingsUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<SettingsUiEffect> = _uiEffect.asSharedFlow()

    private var accessJob: Job? = null

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.PasswordChanged -> onPasswordChanged(event.value)
            SettingsUiEvent.SubmitPassword -> submitPassword()
            SettingsUiEvent.DismissDialog -> emit(SettingsUiEffect.NavigateBack)
            SettingsUiEvent.Reset -> resolveAccess()
        }
    }

    private fun resolveAccess() {
        _uiState.value = SettingsUiState(isResolvingAccess = true)
        accessJob?.cancel()
        accessJob = viewModelScope.launch {
            if (isSettingsPinRequired()) {
                _uiState.update { it.copy(isResolvingAccess = false) }
            } else {
                // No exit PIN set from the staff app: the settings open without asking.
                emit(SettingsUiEffect.Unlocked)
            }
        }
    }

    private fun onPasswordChanged(value: String) {
        if (_uiState.value.isValidating) return
        // The PIN pad only enters digits; cap at the longest valid PIN.
        val pin = value.filter { it.isDigit() }.take(SettingsConstants.MAX_PIN_LENGTH)
        _uiState.update { it.copy(passwordInput = pin, showError = false) }
        if (pin.length == SettingsConstants.MAX_PIN_LENGTH) submitPassword()
    }

    private fun submitPassword() {
        val state = _uiState.value
        if (state.isValidating || state.passwordInput.length < SettingsConstants.MIN_PIN_LENGTH) return
        _uiState.update { it.copy(isValidating = true) }
        viewModelScope.launch {
            val accepted = validateSettingsPassword(state.passwordInput).getOrDefault(false)
            if (accepted) {
                _uiState.update { it.copy(isValidating = false) }
                emit(SettingsUiEffect.Unlocked)
            } else {
                // Clear the entry so the merchant can retype the PIN.
                _uiState.update { it.copy(passwordInput = "", showError = true, isValidating = false) }
            }
        }
    }

    private fun emit(effect: SettingsUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
