package com.qhathia.mobile.discovery.feature.cart.model

/** One-shot intents emitted by the cart dialog. */
sealed interface CartUiEffect {

    /** Open the product detail for the tapped line (the host also dismisses the dialog). */
    data class NavigateToDetail(val productId: String) : CartUiEffect

    /** Dismiss the cart dialog, leaving the basket and underlying screen unchanged. */
    data object Dismiss : CartUiEffect

    /** Dismiss the cart dialog and return to the Home screen (the session has been cleared). */
    data object ReturnHome : CartUiEffect
}
