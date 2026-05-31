package com.qhathia.mobile.discovery.feature.home.model

/**
 * Immutable snapshot of the Discovery Home screen.
 *
 * @property storeName the operating store's name, or `null` when not yet available.
 * @property query the current search-bar text.
 * @property micState the microphone interaction state.
 */
data class HomeUiState(
    val storeName: String? = null,
    val query: String = "",
    val micState: MicState = MicState.Idle,
) {
    /**
     * Business rule: the search bar and the catalog/settings controls are hidden only while the
     * app is actively listening ("Escuchando…"). They remain visible in the idle and no-audio
     * retry states, matching the Figma flow (node 286-9269).
     */
    val areControlsVisible: Boolean get() = micState !is MicState.Listening
}
