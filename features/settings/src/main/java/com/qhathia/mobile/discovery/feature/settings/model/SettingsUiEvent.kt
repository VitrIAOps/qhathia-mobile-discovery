package com.qhathia.mobile.discovery.feature.settings.model

/** User-driven intents on the settings PIN gate. */
sealed interface SettingsUiEvent {

    /** The PIN field changed (keypad digit / backspace). */
    data class PasswordChanged(val value: String) : SettingsUiEvent

    /** Submit the entered PIN for validation (also triggered automatically once it is complete). */
    data object SubmitPassword : SettingsUiEvent

    /** Dismiss the PIN dialog without unlocking (returns to the previous screen). */
    data object DismissDialog : SettingsUiEvent

    /** Reset the gate to a blank PIN — dispatched whenever the gate is shown so it always re-prompts. */
    data object Reset : SettingsUiEvent
}
