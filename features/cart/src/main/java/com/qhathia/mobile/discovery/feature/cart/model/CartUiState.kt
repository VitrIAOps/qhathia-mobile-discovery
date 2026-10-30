package com.qhathia.mobile.discovery.feature.cart.model

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.usecase.ConsumerNameError
import com.qhathia.mobile.discovery.core.domain.usecase.ConsumerPhoneError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * Immutable snapshot of the cart dialog.
 *
 * @property items the cart lines.
 * @property totalItemCount total units across all lines (header subtitle + receipt subtotal count).
 * @property totalAmount total amount to pay.
 * @property name the consumer's entered name.
 * @property phone the consumer's entered cell-phone number.
 * @property nameError the name validation error, or `null` when valid.
 * @property phoneError the phone validation error, or `null` when valid.
 * @property isOrderEnabled whether the consumer data is complete and valid (gates "Realizar pedido").
 * @property submission the order-submission phase overlaid on the dialog (loading / success / error).
 * @property unavailableIds products the server no longer accepts, found by the recheck after a
 *   `400` / `404`. Their lines show "No disponible" and block the order and share actions.
 */
data class CartUiState(
    val items: ImmutableList<CartItem> = persistentListOf(),
    val totalItemCount: Int = 0,
    val totalAmount: Double = 0.0,
    val name: String = "",
    val phone: String = "",
    val nameError: ConsumerNameError? = ConsumerNameError.Empty,
    val phoneError: ConsumerPhoneError? = ConsumerPhoneError.Empty,
    val isOrderEnabled: Boolean = false,
    val submission: OrderSubmission = OrderSubmission.Idle,
    val unavailableIds: ImmutableSet<String> = persistentSetOf(),
) {
    /** True when the basket has no lines. */
    val isEmpty: Boolean get() = items.isEmpty()

    /** True while a line flagged "No disponible" is still in the basket. */
    val hasUnavailableLines: Boolean get() = items.any { it.productId in unavailableIds }

    /**
     * Whether "Realizar pedido" and "Compartir" may run: products in the basket, valid consumer data
     * and no flagged line (CAR-020, CAR-031).
     */
    val canSubmit: Boolean get() = !isEmpty && isOrderEnabled && !hasUnavailableLines

    /** True while an order request is in flight — the dialog blocks all inputs. */
    val isSubmitting: Boolean get() = submission is OrderSubmission.Submitting

    /** Show the name error message only once the field has content (a blank field is not "wrong" yet). */
    val showNameError: Boolean get() = name.isNotBlank() && nameError != null

    /** Show the phone error message only once the field has content. */
    val showPhoneError: Boolean get() = phone.isNotBlank() && phoneError != null
}
