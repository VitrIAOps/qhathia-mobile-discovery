package com.qhathia.mobile.discovery.core.domain.model

/**
 * A single line in the [Cart]: a selected product with the chosen quantity. Framework-free.
 *
 * @property productId stable product identifier (the line's identity — one line per product).
 * @property name display name.
 * @property priceUnit per-unit price in store currency (formatted for display by the UI layer).
 * @property imageUrl primary image URL, or empty when none.
 * @property availability availability status as returned by the API (e.g. `AVAILABLE`).
 * @property quantity the selected quantity (always ≥ 1 while the line exists).
 * @property maxQuantity the maximum selectable quantity, derived from inventory stock.
 */
data class CartItem(
    val productId: String,
    val name: String,
    val priceUnit: Double,
    val imageUrl: String,
    val availability: String,
    val quantity: Int,
    val maxQuantity: Int,
) {
    /** This line's subtotal: [priceUnit] × [quantity]. */
    val lineSubtotal: Double get() = priceUnit * quantity
}
