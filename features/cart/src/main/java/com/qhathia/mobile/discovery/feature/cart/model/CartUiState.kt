package com.qhathia.mobile.discovery.feature.cart.model

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.usecase.ConsumerNameError
import com.qhathia.mobile.discovery.core.domain.usecase.ConsumerPhoneError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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
) {
    /** True when the basket has no lines. */
    val isEmpty: Boolean get() = items.isEmpty()

    /** Show the name error message only once the field has content (a blank field is not "wrong" yet). */
    val showNameError: Boolean get() = name.isNotBlank() && nameError != null

    /** Show the phone error message only once the field has content. */
    val showPhoneError: Boolean get() = phone.isNotBlank() && phoneError != null
}
