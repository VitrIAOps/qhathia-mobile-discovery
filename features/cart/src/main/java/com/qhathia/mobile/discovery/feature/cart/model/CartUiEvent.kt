package com.qhathia.mobile.discovery.feature.cart.model

/** User-driven intents on the cart dialog. */
sealed interface CartUiEvent {

    /** Increase a line's quantity by one (bounded by stock). */
    data class IncreaseLine(val productId: String) : CartUiEvent

    /** Decrease a line's quantity by one (removes the line when it reaches zero). */
    data class DecreaseLine(val productId: String) : CartUiEvent

    /** Remove a line from the cart. */
    data class RemoveLine(val productId: String) : CartUiEvent

    /** Open a line's product detail (dismisses the dialog and navigates). */
    data class OpenLineDetail(val productId: String) : CartUiEvent

    /** The consumer name field changed. */
    data class NameChanged(val name: String) : CartUiEvent

    /** The consumer cell-phone field changed. */
    data class PhoneChanged(val phone: String) : CartUiEvent

    /** Tap "Realizar pedido" — validate and submit the order to the API. */
    data object PlaceOrder : CartUiEvent

    /** Tap "Compartir por WhatsApp" (render-only — no WhatsApp action until it lands). */
    data object ShareWhatsApp : CartUiEvent

    /** Tap "Aceptar" on the success screen — clear the session and return Home. */
    data object AcceptSuccess : CartUiEvent

    /** Tap "Entendido" on the error screen — return to the cart summary to retry. */
    data object DismissError : CartUiEvent

    /** Tap "Cancelar orden" on the error screen — clear the session and return Home. */
    data object CancelOrder : CartUiEvent

    /** Tap "Seguir buscando" — dismiss the dialog, keeping the cart. */
    data object KeepShopping : CartUiEvent

    /** Tap the top "X" — dismiss the dialog, keeping the cart. */
    data object Close : CartUiEvent
}
