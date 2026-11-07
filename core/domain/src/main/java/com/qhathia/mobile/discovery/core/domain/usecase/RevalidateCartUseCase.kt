package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.repository.CartPricingRepository
import javax.inject.Inject

/**
 * Rechecks the basket after the server rejected an order or share for an unavailable product without
 * naming it (`400` / `404` / `409` without a usable body), so the cart can flag the culprits instead of
 * looping on a generic error (CAR-023/025).
 *
 * Reads the store's current products once (`GET /api/v1/products`, which does not count product views)
 * instead of one `GET /products/{id}` per line. A line is unavailable when its product is absent
 * (deleted or deactivated) or can no longer be ordered. When the products cannot be read, nothing is
 * flagged: the recheck only reports what it knows.
 *
 * @return the product ids of the unavailable lines; empty when nothing could be blamed.
 */
class RevalidateCartUseCase @Inject constructor(
    private val pricingRepository: CartPricingRepository,
) {
    suspend operator fun invoke(items: List<CartItem>): Set<String> {
        val snapshots = pricingRepository.snapshots().getOrElse { return emptySet() }
        return items.filter { item ->
            val current = snapshots[item.productId]
            current == null || !IsOrderableUseCase.isOrderable(current.availability, stock = current.stock)
        }.mapTo(mutableSetOf()) { it.productId }
    }
}
