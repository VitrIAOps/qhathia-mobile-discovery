package com.qhathia.mobile.discovery.feature.cart.model

import com.qhathia.mobile.discovery.core.domain.model.ConsumerField
import kotlinx.collections.immutable.ImmutableList

/**
 * A snapshot of one ordered product for the success summary, captured at submit time so it survives the
 * basket being cleared.
 *
 * @property imageUrl the product's primary image URL, or empty when none.
 * @property name the product display name.
 * @property quantity the ordered quantity.
 * @property lineTotal the line total the server recorded, or `null` for a quote-only share.
 */
data class OrderedLine(
    val imageUrl: String,
    val name: String,
    val quantity: Int,
    val lineTotal: Double? = null,
)

/** Why an order failed, which decides the error screen's copy and actions. */
sealed interface OrderFailure {
    /** No connection: the order surely was not created; "Reintentar" is offered. */
    data object Network : OrderFailure

    /** No answer in time: the order may exist, so it is never retried; ask the seller. */
    data object Timeout : OrderFailure

    /** `422 VALIDATION_ERROR`: the server rejected [fields]. */
    data class Validation(val fields: Set<ConsumerField>) : OrderFailure

    /** Any other failure: the simplified message with "Cancelar orden". */
    data object Generic : OrderFailure
}

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
     * @property total the order total the server recorded, or `null` for a quote-only share.
     * @property totalMismatch whether [total] differs from the total the cart showed (by S/ 0.01 or more).
     */
    data class Success(
        val summary: ImmutableList<OrderedLine>,
        val isQuoteOnly: Boolean = false,
        val shareDelivered: Boolean = true,
        val total: Double? = null,
        val totalMismatch: Boolean = false,
    ) : OrderSubmission

    /** The order failed; [reason] decides the copy and the recovery actions. */
    data class Error(val reason: OrderFailure = OrderFailure.Generic) : OrderSubmission

    /**
     * "Compartir por WhatsApp" got an answer, but WhatsApp did not receive the quote (`FAILED` /
     * `STUB`). Shown as an error that sends the consumer to the seller; never as a success.
     */
    data object QuoteNotDelivered : OrderSubmission

    /**
     * "Compartir por WhatsApp" was rejected or failed after its retries. Shown with share copy and a
     * single "Entendido" that returns to the cart; a failed quote never empties the basket (CAR-033).
     *
     * @property network `true` when it failed for lack of connection, which says so (F-34).
     */
    data class ShareError(val network: Boolean = false) : OrderSubmission
}
