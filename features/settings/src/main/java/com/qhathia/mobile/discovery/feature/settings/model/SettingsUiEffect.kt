package com.qhathia.mobile.discovery.feature.settings.model

/** One-shot intents emitted by the settings PIN gate. */
sealed interface SettingsUiEffect {

    /** The correct PIN was entered — the host opens the Network settings screen. */
    data object Unlocked : SettingsUiEffect

    /** Leave the gate (the PIN dialog was dismissed without unlocking). */
    data object NavigateBack : SettingsUiEffect
}
