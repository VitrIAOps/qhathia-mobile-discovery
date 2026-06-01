package com.qhathia.mobile.discovery.feature.home.model

/** One-shot intents emitted by the Home screen. Destination screens are out of scope. */
sealed interface HomeUiEffect {
    data object NavigateToCatalog : HomeUiEffect
    data class NavigateToResults(val query: String) : HomeUiEffect
    data object NavigateToSettings : HomeUiEffect

    /**
     * A voice search produced [transcript]. Navigating to results is out of scope, so for now this
     * is logged/observed; a later change will route it into the product search.
     */
    data class VoiceTranscribed(val transcript: String) : HomeUiEffect
}
