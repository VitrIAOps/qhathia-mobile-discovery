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

    /** Tap "Realizar pedido" (render-only — no order API until it lands). */
    data object PlaceOrder : CartUiEvent

    /** Tap "Compartir por WhatsApp" (render-only — no WhatsApp action until it lands). */
    data object ShareWhatsApp : CartUiEvent

    /** Tap "Seguir buscando" — dismiss the dialog, keeping the cart. */
    data object KeepShopping : CartUiEvent

    /** Tap the top "X" — dismiss the dialog, keeping the cart. */
    data object Close : CartUiEvent
}
