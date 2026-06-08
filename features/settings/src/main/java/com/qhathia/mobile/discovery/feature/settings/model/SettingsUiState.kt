package com.qhathia.mobile.discovery.feature.settings.model

import com.qhathia.mobile.discovery.core.domain.SettingsConstants

/**
 * Immutable snapshot of the settings PIN gate.
 *
 * @property passwordInput the PIN currently entered on the keypad.
 * @property showError whether the last attempt was rejected (shows the inline error).
 */
data class SettingsUiState(
    val passwordInput: String = "",
    val showError: Boolean = false,
) {
    /** A complete PIN (expected length) enables validation. */
    val isSubmitEnabled: Boolean
        get() = passwordInput.length == SettingsConstants.SETTINGS_PASSWORD_LENGTH
}
