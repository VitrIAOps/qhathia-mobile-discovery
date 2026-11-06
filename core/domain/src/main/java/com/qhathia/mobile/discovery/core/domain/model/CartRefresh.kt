package com.qhathia.mobile.discovery.core.domain.model

/**
 * The store's current view of one product, used to refresh a basket line.
 *
 * @property unitPrice the price the customer pays now (`discountedPrice` when set, else `price`).
 * @property stock units in stock, or `null` when the product has no stock count.
 */
data class ProductSnapshot(
    val id: String,
    val unitPrice: Double,
    val availability: String,
    val stock: Int?,
)

/** A basket line whose unit price changed from [oldUnitPrice] to [newUnitPrice]. */
data class CartPriceChange(
    val productId: String,
    val productName: String,
    val oldUnitPrice: Double,
    val newUnitPrice: Double,
)

/** A basket line lowered to [max] units because that is the stock left. */
data class CartQuantityCap(
    val productId: String,
    val productName: String,
    val max: Int,
)

/**
 * The result of refreshing the basket against the store ([Cart.refreshedWith]).
 *
 * @property cart the refreshed basket.
 * @property priceChanges lines whose unit price changed.
 * @property removedNames names of the lines removed because they can no longer be ordered.
 * @property capped lines lowered to the stock left.
 */
data class CartRefresh(
    val cart: Cart,
    val priceChanges: List<CartPriceChange> = emptyList(),
    val removedNames: List<String> = emptyList(),
    val capped: List<CartQuantityCap> = emptyList(),
) {
    /** Whether anything in the basket changed on the customer's behalf. */
    val hasChanges: Boolean
        get() = priceChanges.isNotEmpty() || removedNames.isNotEmpty() || capped.isNotEmpty()
}
