package com.qhathia.mobile.discovery.feature.productsearch.model

/** User-driven intents on the product-search screen. */
sealed interface ProductSearchUiEvent {

    /** Go back to the previous result, or (when none) ask to close the current search. */
    data object Back : ProductSearchUiEvent

    /** Tap "Nueva búsqueda" — opens the close-confirmation dialog. */
    data object NewSearch : ProductSearchUiEvent

    /** Tap "Ir al catálogo" — opens the catalog directly, keeping the cart (no dialog). */
    data object GoToCatalog : ProductSearchUiEvent

    /** Confirm closing the current search — ends the customer session, clears history and returns Home. */
    data object ConfirmCloseSearch : ProductSearchUiEvent

    /** Dismiss the confirmation dialog, keeping the current search. */
    data object DismissDialog : ProductSearchUiEvent

    /** The bottom search-bar text changed. */
    data class QueryChanged(val query: String) : ProductSearchUiEvent

    /** Submit the bottom search-bar query as a new text search. */
    data object SubmitSearch : ProductSearchUiEvent

    /** Tap the bottom microphone to start a new voice search in place. */
    data object ActivateMic : ProductSearchUiEvent

    /** The consumer finished speaking — stop recording and transcribe. */
    data object StopRecording : ProductSearchUiEvent

    /** Cancel the in-place voice capture and restore the previous screen. */
    data object CancelVoice : ProductSearchUiEvent

    /** Tap a product's cart button — adds that product to the cart. */
    data class CartTapped(val productId: String) : ProductSearchUiEvent

    /** Tap the top-bar Ir al carrito control — opens the cart dialog. */
    data object OpenCart : ProductSearchUiEvent

    /** Tap a product card (its surface, not the cart button) — opens the product detail. */
    data class ProductTapped(val productId: String) : ProductSearchUiEvent

    /** Toggle a filter option ([criterion] + [option]); re-tapping a selected option de-selects it. */
    data class FilterToggled(val criterion: String, val option: String) : ProductSearchUiEvent

    /** Clear every selected option under [criterion] (the section's "Limpiar" link). */
    data class FilterCriterionCleared(val criterion: String) : ProductSearchUiEvent

    /** Select a suggestion [phrase]; re-tapping the active suggestion de-selects it. */
    data class SuggestionSelected(val phrase: String) : ProductSearchUiEvent

    /** The screen is hidden (lifecycle `ON_STOP`): voice capture in progress is cancelled. */
    data object ScreenStopped : ProductSearchUiEvent

    /** The screen is shown again (lifecycle `ON_RESUME`). */
    data object ScreenResumed : ProductSearchUiEvent

    /** "Reintentar" on the network error: repeat the last search, chips included (F-38). */
    data object Retry : ProductSearchUiEvent
}
