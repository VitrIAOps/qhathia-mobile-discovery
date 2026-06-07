package com.qhathia.mobile.discovery.feature.productsearch.model

/** User-driven intents on the product-search screen. */
sealed interface ProductSearchUiEvent {

    /** Go back to the previous result, or (when none) ask to close the current search. */
    data object Back : ProductSearchUiEvent

    /** Tap "Nueva búsqueda" — opens the close-confirmation dialog. */
    data object NewSearch : ProductSearchUiEvent

    /** Confirm closing the current search — clears history and returns to Home idle. */
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

    /** Tap a product's cart button (no-op until cart navigation lands). */
    data class CartTapped(val productId: String) : ProductSearchUiEvent

    /** Tap a product card (its surface, not the cart button) — opens the product detail. */
    data class ProductTapped(val productId: String) : ProductSearchUiEvent
}
