package com.qhathia.mobile.discovery.feature.cart.model

import kotlinx.collections.immutable.ImmutableList

/**
 * A snapshot of one ordered product for the success summary, captured at submit time so it survives the
 * basket being cleared.
 *
 * @property imageUrl the product's primary image URL, or empty when none.
 * @property name the product display name.
 * @property quantity the ordered quantity.
 */
data class OrderedLine(
    val imageUrl: String,
    val name: String,
    val quantity: Int,
)

/**
 * The order-submission phase overlaid on the cart dialog. The stateless content renders purely from
 * this: [Idle] shows the normal cart, [Submitting] blocks inputs behind a loader, [Success] shows the
 * confirmation summary, and [Error] shows the simplified error with its recovery actions.
 */
sealed interface OrderSubmission {

    /** No order in progress — the normal cart is interactive. */
    data object Idle : OrderSubmission

    /** An order request is in flight — all inputs are blocked behind a centered loader. */
    data object Submitting : OrderSubmission

    /** The order was created — [summary] lists the ordered products for the confirmation screen. */
    data class Success(val summary: ImmutableList<OrderedLine>) : OrderSubmission

    /** The order failed — the simplified error screen with "Entendido" / "Cancelar orden" is shown. */
    data object Error : OrderSubmission
}
