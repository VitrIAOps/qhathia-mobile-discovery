package com.qhathia.mobile.discovery.feature.home.model

/** User-driven intents on the Home screen. */
sealed interface HomeUiEvent {
    /** Start a voice-search listening window. */
    data object ActivateMic : HomeUiEvent

    /** Cancel the current microphone interaction and return to idle. */
    data object CancelMic : HomeUiEvent

    /** Retry listening after a no-audio timeout. */
    data object RetryMic : HomeUiEvent

    /** The search query text changed. */
    data class QueryChanged(val query: String) : HomeUiEvent

    /** Submit the current search query. */
    data object SubmitSearch : HomeUiEvent

    /** Open the catalog. */
    data object OpenCatalog : HomeUiEvent

    /** Open settings. */
    data object OpenSettings : HomeUiEvent
}
