package com.qhathia.mobile.discovery.feature.settings.model

/** One-shot intents emitted by the Network settings screen. */
sealed interface NetworkSettingsUiEffect {

    /** Leave the Network settings screen. */
    data object NavigateBack : NetworkSettingsUiEffect
}
