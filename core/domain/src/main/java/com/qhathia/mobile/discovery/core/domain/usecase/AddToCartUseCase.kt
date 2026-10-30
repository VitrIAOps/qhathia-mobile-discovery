package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.CartConstants
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.repository.CartRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** What happened when a product was added to the cart. */
enum class AddToCartResult {
    /** The product was added, or merged into its existing line (quantity capped by stock). */
    Added,

    /** The product is not `AVAILABLE`; nothing was added. */
    NotOrderable,

    /** The basket already has [CartConstants.MAX_DISTINCT_LINES] other products; nothing was added. */
    LineLimitReached,
}

/**
 * Adds a product to the cart. Adding the same product again merges into its existing line; the
 * quantity is bounded by the product's stock cap (enforced by the [com.qhathia.mobile.discovery.core.domain.model.Cart]
 * transform the repository applies).
 */
class AddToCartUseCase @Inject constructor(
    private val repository: CartRepository,
) {
    /**
     * Adds [item] when its product is orderable (`AVAILABLE`) and fits in the basket. The screens also
     * disable their add controls for products that are not orderable, so this is the last guard against
     * a basket the server would reject: `400` for a product that is not orderable, `422` for more than
     * [CartConstants.MAX_DISTINCT_LINES] lines (wave 2, CAR-021). More units of a product already in the
     * basket are always accepted.
     */
    suspend operator fun invoke(item: CartItem): AddToCartResult {
        if (!IsOrderableUseCase.isOrderable(item.availability)) return AddToCartResult.NotOrderable
        val lines = repository.observe().first().items
        val isNewLine = lines.none { it.productId == item.productId }
        if (isNewLine && lines.size >= CartConstants.MAX_DISTINCT_LINES) return AddToCartResult.LineLimitReached
        repository.addProduct(item)
        return AddToCartResult.Added
    }
}
