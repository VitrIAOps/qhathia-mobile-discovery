package com.qhathia.mobile.discovery.feature.productsearch.model

/** One-shot intents emitted by the product-search screen. */
sealed interface ProductSearchUiEffect {

    /** Return to the Home idle screen (confirmed new search, unhappy idle timeout, or back-to-close). */
    data object NavigateHomeIdle : ProductSearchUiEffect
}
