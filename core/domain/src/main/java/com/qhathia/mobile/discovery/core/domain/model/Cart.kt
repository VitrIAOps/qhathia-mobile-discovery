package com.qhathia.mobile.discovery.core.domain.model

import com.qhathia.mobile.discovery.core.domain.CartConstants
import com.qhathia.mobile.discovery.core.domain.usecase.IsOrderableUseCase
import java.math.BigDecimal
import java.math.RoundingMode

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

    /**
     * Refreshes every line against the store's current [snapshots] (keyed by product id). A line whose
     * product is absent (deleted or deactivated) or can no longer be ordered is removed; otherwise its
     * price, availability, stock and cap are updated, and a quantity above the new cap is lowered.
     * Prices are compared at 2 decimals (half up), the precision the server checks.
     */
    fun refreshedWith(snapshots: Map<String, ProductSnapshot>): CartRefresh {
        val kept = mutableListOf<CartItem>()
        val priceChanges = mutableListOf<CartPriceChange>()
        val removed = mutableListOf<String>()
        val capped = mutableListOf<CartQuantityCap>()
        for (item in items) {
            val current = snapshots[item.productId]
            if (current == null || !IsOrderableUseCase.isOrderable(current.availability, stock = current.stock)) {
                removed += item.name
                continue
            }
            if (current.unitPrice.toMoney() != item.priceUnit.toMoney()) {
                priceChanges += CartPriceChange(item.productId, item.name, item.priceUnit, current.unitPrice)
            }
            val max = CartConstants.maxQuantityFor(current.availability, current.stock)
            if (item.quantity > max) capped += CartQuantityCap(item.productId, item.name, max)
            kept += item.copy(
                priceUnit = current.unitPrice,
                availability = current.availability,
                stock = current.stock,
                maxQuantity = max,
                quantity = item.quantity.coerceAtMost(max),
            )
        }
        return CartRefresh(copy(items = kept), priceChanges, removed, capped)
    }

    /** Returns a copy with each changed line set to its new unit price; other lines are untouched. */
    fun withPricesUpdated(changes: List<CartPriceChange>): Cart {
        val byId = changes.associateBy { it.productId }
        return copy(
            items = items.map { item -> byId[item.productId]?.let { item.copy(priceUnit = it.newUnitPrice) } ?: item },
        )
    }

    companion object {
        /** Well-defined empty basket. */
        val EMPTY: Cart = Cart()
    }
}

/** This amount at the 2-decimal precision the server compares (half up). */
fun Double.toMoney(): BigDecimal = BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP)
