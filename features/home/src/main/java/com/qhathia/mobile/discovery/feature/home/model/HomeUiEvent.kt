package com.qhathia.mobile.discovery.feature.home.model

/** User-driven intents on the Home screen. */
sealed interface HomeUiEvent {
    /** Start a voice-search attempt (microphone permission already granted). */
    data object ActivateMic : HomeUiEvent

    /** The consumer finished speaking — stop recording and transcribe. */
    data object StopRecording : HomeUiEvent

    /** Cancel the current voice-search interaction and return to idle. */
    data object CancelMic : HomeUiEvent

    /** Retry a voice-search attempt after a no-audio outcome or an error screen. */
    data object RetryMic : HomeUiEvent

    /** The search query text changed. */
    data class QueryChanged(val query: String) : HomeUiEvent

    /** Submit the current search query. */
    data object SubmitSearch : HomeUiEvent

    /** Open the catalog. */
    data object OpenCatalog : HomeUiEvent

    /** Open settings. */
    data object OpenSettings : HomeUiEvent

    /** Home is no longer visible (lifecycle `ON_STOP`), e.g. after navigating to the search results. */
    data object ScreenStopped : HomeUiEvent
}
