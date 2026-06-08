package com.qhathia.mobile.discovery.core.data.settings

import com.qhathia.mobile.discovery.core.domain.SettingsConstants
import javax.inject.Inject

/**
 * Mocked settings-password data source. Validates against [SettingsConstants.MOCK_SETTINGS_PASSWORD]
 * until a real backend password API exists. Keeping the mock here (and the constant in the domain)
 * means the swap to a real remote/local source is a single-file change.
 */
class MockSettingsPasswordDataSource @Inject constructor() {

    /** Returns `true` when [input] matches the mocked settings password. */
    fun validate(input: String): Boolean = input == SettingsConstants.MOCK_SETTINGS_PASSWORD
}
