package com.qhathia.mobile.discovery.feature.productdetail.model

/** User-driven intents on the product-detail screen. */
sealed interface ProductDetailUiEvent {

    /** Retry loading the detail after an error. */
    data object Retry : ProductDetailUiEvent

    /** Tap "Volver" — returns to the search results. */
    data object Back : ProductDetailUiEvent

    /** Tap "Nueva búsqueda" — opens the close-confirmation dialog. */
    data object NewSearch : ProductDetailUiEvent

    /** Confirm closing the current search — returns to Home idle. */
    data object ConfirmCloseSearch : ProductDetailUiEvent

    /** Dismiss the confirmation dialog, keeping the current detail. */
    data object DismissDialog : ProductDetailUiEvent

    /** Tap "Agregar al carrito" — adds the product to the cart with the selected quantity. */
    data object AddToCart : ProductDetailUiEvent

    /** Increase the add-to-cart stepper quantity (bounded by stock). */
    data object IncreaseQuantity : ProductDetailUiEvent

    /** Decrease the add-to-cart stepper quantity (floored at 1). */
    data object DecreaseQuantity : ProductDetailUiEvent

    /** Tap the top-bar Ir al carrito control — opens the cart dialog. */
    data object OpenCart : ProductDetailUiEvent

    /** Tap "Compartir por WhatsApp" (render-only — no WhatsApp navigation until it lands). */
    data object ShareWhatsApp : ProductDetailUiEvent

    /** Tap the bottom microphone (render-only — in-place voice on detail is out of scope). */
    data object ActivateMic : ProductDetailUiEvent

    /** The bottom search-bar text changed. */
    data class QueryChanged(val query: String) : ProductDetailUiEvent

    /** Submit the bottom search-bar query as a new text search. */
    data object SubmitSearch : ProductDetailUiEvent

    /** Tap a related product (mock — no navigation until the related-products API lands). */
    data class RelatedProductTapped(val productId: String) : ProductDetailUiEvent
}
