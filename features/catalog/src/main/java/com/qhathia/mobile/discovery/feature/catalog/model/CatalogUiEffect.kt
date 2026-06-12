package com.qhathia.mobile.discovery.feature.catalog.model

import com.qhathia.mobile.discovery.core.domain.model.SearchSource

/** One-shot intents emitted by the catalog screen. */
sealed interface CatalogUiEffect {

    /** Open the product-detail screen for the tapped product. */
    data class NavigateToDetail(val productId: String) : CatalogUiEffect

    /** Open the cart dialog (hosted by the app over the current screen). */
    data object OpenCart : CatalogUiEffect

    /** Return to the Home idle screen (confirmed close, back with empty cart, or error idle timeout). */
    data object NavigateHomeIdle : CatalogUiEffect

    /** Hand off to the product-search flow, carrying the query and its source. */
    data class NavigateToSearch(val query: String, val source: SearchSource) : CatalogUiEffect
}
