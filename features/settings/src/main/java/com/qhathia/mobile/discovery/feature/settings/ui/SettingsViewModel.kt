package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.SettingsConstants
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateSettingsPasswordUseCase
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEffect
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEvent
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
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
 * Drives the settings PIN gate. The PIN pad feeds digits in through [SettingsUiEvent.PasswordChanged];
 * once the input reaches the expected length it is validated automatically through
 * [ValidateSettingsPasswordUseCase]. A correct PIN emits [SettingsUiEffect.Unlocked] (the host then
 * opens the Network settings screen); a wrong one clears the entry and shows an inline error. The gate
 * re-prompts on every entry via [SettingsUiEvent.Reset].
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val validateSettingsPassword: ValidateSettingsPasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SettingsUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<SettingsUiEffect> = _uiEffect.asSharedFlow()

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.PasswordChanged -> onPasswordChanged(event.value)
            SettingsUiEvent.SubmitPassword -> submitPassword()
            SettingsUiEvent.DismissDialog -> emit(SettingsUiEffect.NavigateBack)
            SettingsUiEvent.Reset -> _uiState.value = SettingsUiState()
        }
    }

    private fun onPasswordChanged(value: String) {
        // The PIN pad only enters digits; cap at the expected length.
        val pin = value.filter { it.isDigit() }.take(SettingsConstants.SETTINGS_PASSWORD_LENGTH)
        _uiState.update { it.copy(passwordInput = pin, showError = false) }
        // A complete PIN validates automatically (there is no separate submit button).
        if (pin.length == SettingsConstants.SETTINGS_PASSWORD_LENGTH) submitPassword()
    }

    private fun submitPassword() {
        val input = _uiState.value.passwordInput
        viewModelScope.launch {
            validateSettingsPassword(input)
                .onSuccess { accepted ->
                    if (accepted) {
                        emit(SettingsUiEffect.Unlocked)
                    } else {
                        // Clear the entry so the merchant can retype the PIN.
                        _uiState.update { it.copy(passwordInput = "", showError = true) }
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(passwordInput = "", showError = true) }
                }
        }
    }

    private fun emit(effect: SettingsUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
