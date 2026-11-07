package com.qhathia.mobile.discovery.feature.cart.model

import com.qhathia.mobile.discovery.core.domain.model.CartPriceChange
import com.qhathia.mobile.discovery.core.domain.model.CartQuantityCap
import com.qhathia.mobile.discovery.core.domain.model.CartRefresh

/**
 * What changed in the basket on the customer's behalf, shown above the totals: new prices, products
 * removed because they can no longer be ordered, and quantities lowered to the stock left.
 */
data class CartNotice(
    val priceChanges: List<CartPriceChange> = emptyList(),
    val removedNames: List<String> = emptyList(),
    val capped: List<CartQuantityCap> = emptyList(),
) {
    val isEmpty: Boolean get() = priceChanges.isEmpty() && removedNames.isEmpty() && capped.isEmpty()

    /** This notice with [other] added; a newer entry for the same product replaces the older one. */
    operator fun plus(other: CartNotice): CartNotice = CartNotice(
        priceChanges = (priceChanges.filter { old -> other.priceChanges.none { it.productId == old.productId } } + other.priceChanges),
        removedNames = (removedNames + other.removedNames).distinct(),
        capped = (capped.filter { old -> other.capped.none { it.productId == old.productId } } + other.capped),
    )

    companion object {
        fun of(refresh: CartRefresh): CartNotice =
            CartNotice(refresh.priceChanges, refresh.removedNames, refresh.capped)
    }
}

/** [notice] merged into this one, or `null` when both are empty. */
fun CartNotice?.merge(notice: CartNotice): CartNotice? =
    ((this ?: CartNotice()) + notice).takeUnless { it.isEmpty }
