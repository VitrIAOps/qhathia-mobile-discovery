package com.qhathia.mobile.discovery.core.domain.model

/**
 * The session-scoped order-request basket. Framework-free domain model that owns the cart's business
 * rules as pure transformations, so the data layer and test fakes share a single source of truth and
 * cannot drift.
 *
 * @property items the cart lines, one per product, in insertion order.
 */
data class Cart(
    val items: List<CartItem> = emptyList(),
) {
    /** Total number of units across all lines (the badge / subtitle count). */
    val totalItemCount: Int get() = items.sumOf { it.quantity }

    /** Total amount to pay: the sum of every line subtotal. */
    val totalAmount: Double get() = items.sumOf { it.lineSubtotal }

    /** True when the basket has no lines. */
    val isEmpty: Boolean get() = items.isEmpty()

    /**
     * Returns a copy with [item] added. If the product is already in the cart its quantity is
     * increased (merged into the existing line); otherwise a new line is appended. The resulting
     * quantity is capped at the line's [CartItem.maxQuantity] (stock bound). There is no limit on the
     * number of distinct products.
     */
    fun withProductAdded(item: CartItem): Cart {
        val existing = items.firstOrNull { it.productId == item.productId }
        return if (existing == null) {
            copy(items = items + item.copy(quantity = item.quantity.coerceIn(1, item.maxQuantity)))
        } else {
            val merged = (existing.quantity + item.quantity).coerceIn(1, item.maxQuantity)
            copy(items = items.map { if (it.productId == item.productId) it.copy(quantity = merged, maxQuantity = item.maxQuantity) else it })
        }
    }

    /**
     * Returns a copy with the line for [productId] set to [quantity], capped at its
     * [CartItem.maxQuantity]. A resulting quantity below 1 removes the line. Unknown products are
     * left untouched.
     */
    fun withQuantitySet(productId: String, quantity: Int): Cart {
        val existing = items.firstOrNull { it.productId == productId } ?: return this
        val bounded = quantity.coerceAtMost(existing.maxQuantity)
        return if (bounded < 1) {
            withProductRemoved(productId)
        } else {
            copy(items = items.map { if (it.productId == productId) it.copy(quantity = bounded) else it })
        }
    }

    /** Returns a copy with the line for [productId] removed (no-op if absent). */
    fun withProductRemoved(productId: String): Cart =
        copy(items = items.filterNot { it.productId == productId })

    companion object {
        /** Well-defined empty basket. */
        val EMPTY: Cart = Cart()
    }
}
