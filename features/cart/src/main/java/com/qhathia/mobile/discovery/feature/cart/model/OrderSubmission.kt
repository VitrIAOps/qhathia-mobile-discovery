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

    /**
     * The submission succeeded — [summary] lists the products for the confirmation screen.
     *
     * @property isQuoteOnly `true` for "Compartir por WhatsApp" (a quote was sent, no order exists),
     *   which shows the quote-sent copy; `false` for "Realizar pedido", which shows the order copy.
     * @property shareDelivered whether the WhatsApp quote reached the consumer (`status = SENT`).
     *   Always `true` for a quote-only success. For "Realizar pedido", `false` means the order was
     *   created but the quote was not delivered, which switches the copy to the "go to the seller and
     *   give your name" fallback instead of "check your phone".
     */
    data class Success(
        val summary: ImmutableList<OrderedLine>,
        val isQuoteOnly: Boolean = false,
        val shareDelivered: Boolean = true,
    ) : OrderSubmission

    /** The order failed — the simplified error screen with "Entendido" / "Cancelar orden" is shown. */
    data object Error : OrderSubmission

    /**
     * "Compartir por WhatsApp" got an answer, but WhatsApp did not receive the quote (`FAILED` /
     * `STUB`). Shown as an error that sends the consumer to the seller; never as a success.
     */
    data object QuoteNotDelivered : OrderSubmission
}
