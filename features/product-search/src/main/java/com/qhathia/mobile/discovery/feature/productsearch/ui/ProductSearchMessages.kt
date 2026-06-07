package com.qhathia.mobile.discovery.feature.productsearch.ui

import androidx.annotation.StringRes
import com.qhathia.mobile.discovery.feature.productsearch.R
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchPhase

/**
 * Resolves a processing [ProductSearchPhase] to its animated message base text resource (the
 * three-dot indicator is appended by `ProcessingMessage`). Returns `null` for settled phases that do
 * not show a processing message. Kept in `features:product-search` (which sees both the phase and the
 * string resources) so `core:ui` stays domain-free, mirroring `features:home`'s `SttErrorMessages`.
 */
@StringRes
internal fun ProductSearchPhase.processingTextRes(): Int? = when (this) {
    ProductSearchPhase.VoiceListening -> R.string.ps_listening
    ProductSearchPhase.VoiceTranscribing -> R.string.ps_organizing
    ProductSearchPhase.VoiceSlowResponse -> R.string.ps_slow_stt
    ProductSearchPhase.Searching -> R.string.ps_searching
    ProductSearchPhase.SlowSearch -> R.string.ps_slow_search
    ProductSearchPhase.SearchingAlternatives -> R.string.ps_searching_alternatives
    is ProductSearchPhase.Results,
    is ProductSearchPhase.Unhappy,
    ProductSearchPhase.NetworkError,
    -> null
}
