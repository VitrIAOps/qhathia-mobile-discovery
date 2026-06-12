package com.qhathia.mobile.discovery.feature.catalog.model

/** User-driven intents on the catalog screen. */
sealed interface CatalogUiEvent {

    /** Load (or reload, after an error) the catalog. */
    data object Retry : CatalogUiEvent

    /** Select a category tab — filters the product grid to that tab's products. */
    data class TabSelected(val tabId: String) : CatalogUiEvent

    /** Tap a product card (its surface, not the cart button) — opens the product detail. */
    data class ProductTapped(val productId: String) : CatalogUiEvent

    /** Tap a product's cart button — adds that product to the cart. */
    data class CartTapped(val productId: String) : CatalogUiEvent

    /** Tap the top-bar Ir al carrito control — opens the cart dialog. */
    data object OpenCart : CatalogUiEvent

    /** Go back to the previous state, or (when none) close the session / return Home. */
    data object Back : CatalogUiEvent

    /** Tap "Nueva búsqueda" — opens the close-confirmation dialog. */
    data object NewSearch : CatalogUiEvent

    /** Confirm closing the current session — returns to Home idle. */
    data object ConfirmCloseSearch : CatalogUiEvent

    /** Dismiss the confirmation dialog, keeping the catalog. */
    data object DismissDialog : CatalogUiEvent

    /** The bottom search-bar text changed. */
    data class QueryChanged(val query: String) : CatalogUiEvent

    /** Submit the bottom search-bar query — hands off to the product-search flow (text). */
    data object SubmitSearch : CatalogUiEvent

    /** Tap the bottom microphone — hands off to the product-search flow (voice). */
    data object ActivateMic : CatalogUiEvent
}
