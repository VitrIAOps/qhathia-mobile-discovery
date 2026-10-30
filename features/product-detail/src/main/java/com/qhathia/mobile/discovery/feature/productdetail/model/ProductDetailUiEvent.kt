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

    /** Tap "Compartir por WhatsApp" — opens the share dialog to collect the consumer's name + phone. */
    data object ShareWhatsApp : ProductDetailUiEvent

    /** The share dialog's consumer name field changed. */
    data class ShareNameChanged(val name: String) : ProductDetailUiEvent

    /** The share dialog's consumer cell-phone field changed. */
    data class SharePhoneChanged(val phone: String) : ProductDetailUiEvent

    /** Tap "Compartir" in the dialog — validate and send the quote PDF for this product. */
    data object ConfirmShare : ProductDetailUiEvent

    /** Dismiss the share dialog without sending (the "X" / "Cancelar"). Ignored while sending. */
    data object DismissShare : ProductDetailUiEvent

    /**
     * A tap outside the share dialog or the system Back. Ignored while sending; on a result screen it
     * runs that screen's main action (wave 2, D6).
     */
    data object ShareDismissRequest : ProductDetailUiEvent

    /** Tap "Aceptar" on the share success screen — close the dialog and reset its form. */
    data object AcceptShareSuccess : ProductDetailUiEvent

    /** Tap "Entendido" on the share error screen — return to the form to retry. */
    data object DismissShareError : ProductDetailUiEvent

    /** Tap the bottom microphone (render-only — in-place voice on detail is out of scope). */
    data object ActivateMic : ProductDetailUiEvent

    /** The bottom search-bar text changed. */
    data class QueryChanged(val query: String) : ProductDetailUiEvent

    /** Submit the bottom search-bar query as a new text search. */
    data object SubmitSearch : ProductDetailUiEvent

    /** Tap a related product — opens that product's own detail. */
    data class RelatedProductTapped(val productId: String) : ProductDetailUiEvent

    /** Tap a related product's add-to-cart button — adds that product to the cart (quantity 1). */
    data class RelatedProductAddToCart(val productId: String) : ProductDetailUiEvent
}
