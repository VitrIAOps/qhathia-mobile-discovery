package com.qhathia.mobile.discovery.feature.productsearch.model

import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory

/** One-shot intents emitted by the product-search screen. */
sealed interface ProductSearchUiEffect {

    /** Return to the Home idle screen (confirmed new search, unhappy idle timeout, or back-to-close). */
    data object NavigateHomeIdle : ProductSearchUiEffect

    /**
     * Return to the previous screen. Used when the screen was opened by voice with no query (Catalog
     * microphone) and the capture heard nothing, failed or was cancelled: there are no results to show.
     */
    data object NavigateBack : ProductSearchUiEffect

    /**
     * Volver (or system Back) with no previous result to show (F-38). The screen decides: opened from
     * Home, it asks to close the search (the session ends); opened from any other screen (Catalog,
     * Detail), it goes back there and keeps the cart.
     */
    data object ExitRequested : ProductSearchUiEffect

    /** Open the catalog ("Ir al catálogo"); the cart and the customer session are kept. */
    data object NavigateToCatalog : ProductSearchUiEffect

    /** Open the product-detail screen for the tapped product. */
    data class NavigateToDetail(val productId: String) : ProductSearchUiEffect

    /** Open the cart dialog (hosted by the app over the current screen). */
    data object OpenCart : ProductSearchUiEffect

    /**
     * The voice search failed (F-17): tell the consumer why. The screen under it is kept (or left,
     * when it was opened by voice), so the message must outlive it.
     */
    data class VoiceFailed(val category: SttErrorCategory) : ProductSearchUiEffect

    /** The basket already has 20 distinct products: tell the consumer why nothing was added. */
    data object LineLimitReached : ProductSearchUiEffect
}
