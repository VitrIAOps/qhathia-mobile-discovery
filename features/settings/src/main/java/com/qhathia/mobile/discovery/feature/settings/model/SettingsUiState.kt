package com.qhathia.mobile.discovery.feature.settings.model

import com.qhathia.mobile.discovery.core.domain.SettingsConstants

/**
 * Immutable snapshot of the settings PIN gate.
 *
 * @property passwordInput the PIN currently entered on the keypad (0–6 digits).
 * @property showError whether the last attempt was rejected (shows the inline error).
 * @property isResolvingAccess whether the gate is still checking if a PIN is required; the dialog is
 *   not shown meanwhile (and never, when no PIN is set).
 * @property isValidating a PIN is being checked; the keypad ignores input meanwhile.
 */
data class SettingsUiState(
    val passwordInput: String = "",
    val showError: Boolean = false,
    val isResolvingAccess: Boolean = true,
    val isValidating: Boolean = false,
) {
    /** The confirm key enables once the shortest valid PIN is entered. */
    val isSubmitEnabled: Boolean
        get() = !isValidating && passwordInput.length >= SettingsConstants.MIN_PIN_LENGTH

    /** One dot per entered digit, never fewer than the shortest PIN (so its length is not revealed). */
    val pinDotCount: Int
        get() = maxOf(SettingsConstants.MIN_PIN_LENGTH, passwordInput.length)
}
