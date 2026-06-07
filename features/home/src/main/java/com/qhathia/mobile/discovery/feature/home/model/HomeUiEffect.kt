package com.qhathia.mobile.discovery.feature.home.model

import com.qhathia.mobile.discovery.core.domain.model.SearchSource

/** One-shot intents emitted by the Home screen. Destination screens are out of scope. */
sealed interface HomeUiEffect {
    data object NavigateToCatalog : HomeUiEffect
    data object NavigateToSettings : HomeUiEffect

    /**
     * Start a product search for [query] originating from [source] — emitted both when the consumer
     * submits a typed query (source = [SearchSource.Text]) and when a voice search produces a
     * transcript (source = [SearchSource.Voice]).
     */
    data class NavigateToProductSearch(
        val query: String,
        val source: SearchSource,
    ) : HomeUiEffect
}
