package com.qhathia.mobile.discovery.feature.productdetail.model

/** One-shot intents emitted by the product-detail screen. */
sealed interface ProductDetailUiEffect {

    /** Return to the previous screen (the search results). */
    data object NavigateBack : ProductDetailUiEffect

    /** Return to the Home idle screen (confirmed new search). */
    data object NavigateHomeIdle : ProductDetailUiEffect

    /** Start a new text search from the bottom search bar. */
    data class NavigateToSearch(val query: String) : ProductDetailUiEffect
}
