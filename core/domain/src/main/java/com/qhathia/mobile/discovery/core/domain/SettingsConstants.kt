package com.qhathia.mobile.discovery.core.domain

/**
 * Single source of truth for the settings-access rules. The unlock password is **mocked** here until
 * a real backend password API exists; it is isolated behind [com.qhathia.mobile.discovery.core.domain.repository.SettingsAccessRepository]
 * so swapping in the real validation is a `core:data` change only and the value never leaks into UI.
 */
object SettingsConstants {

    /** Mocked settings-unlock password. TODO(settings-auth): replace with the real password API. */
    const val MOCK_SETTINGS_PASSWORD: String = "1234"

    /** Expected password length — the submit action stays disabled until the input reaches it. */
    const val SETTINGS_PASSWORD_LENGTH: Int = 4
}
