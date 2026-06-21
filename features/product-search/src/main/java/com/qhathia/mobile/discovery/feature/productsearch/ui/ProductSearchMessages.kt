package com.qhathia.mobile.discovery.feature.productsearch.ui

import androidx.annotation.StringRes
import com.qhathia.mobile.discovery.feature.productsearch.R
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchPhase
import com.qhathia.mobile.discovery.core.ui.R as UiR

/**
 * Resolves a processing [ProductSearchPhase] to its animated message base text resource (the
 * three-dot indicator is appended by `ProcessingMessage`). Returns `null` for settled phases that do
 * not show a processing message. Kept in `features:product-search` (which sees both the phase and the
 * string resources) so `core:ui` stays domain-free, mirroring `features:home`'s `SttErrorMessages`.
 */
@StringRes
internal fun ProductSearchPhase.processingTextRes(): Int? = when (this) {
    ProductSearchPhase.VoiceListening -> UiR.string.discovery_voice_listening
    ProductSearchPhase.VoiceTranscribing -> UiR.string.discovery_voice_organizing
    ProductSearchPhase.VoiceSlowResponse -> UiR.string.discovery_voice_slow_response
    ProductSearchPhase.Searching -> R.string.ps_searching
    ProductSearchPhase.SlowSearch -> R.string.ps_slow_search
    is ProductSearchPhase.Results,
    is ProductSearchPhase.Unhappy,
    ProductSearchPhase.NetworkError,
    -> null
}
