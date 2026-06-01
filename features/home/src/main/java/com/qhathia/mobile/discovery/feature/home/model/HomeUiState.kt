package com.qhathia.mobile.discovery.feature.home.model

/**
 * Immutable snapshot of the Discovery Home screen.
 *
 * @property storeName the operating store's name, or `null` when not yet available.
 * @property query the current search-bar text.
 * @property micState the voice-search interaction state.
 */
data class HomeUiState(
    val storeName: String? = null,
    val query: String = "",
    val micState: MicState = MicState.Idle,
) {
    /**
     * The search bar and the catalog/settings controls are hidden only while a voice-search attempt
     * is actively capturing or processing ([MicState.Listening], [MicState.Transcribing],
     * [MicState.SlowResponse]). They are visible in the resting states — including the error screen,
     * which mirrors the no-audio-retry layout (headline + mic + search bar).
     */
    val areControlsVisible: Boolean
        get() = when (micState) {
            is MicState.Listening,
            MicState.Transcribing,
            MicState.SlowResponse,
            -> false

            MicState.Idle,
            is MicState.NoAudioRetry,
            is MicState.Success,
            is MicState.Error,
            -> true
        }
}
