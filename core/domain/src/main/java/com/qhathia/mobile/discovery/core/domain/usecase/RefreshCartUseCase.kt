package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.CartRefresh
import com.qhathia.mobile.discovery.core.domain.repository.CartPricingRepository
import com.qhathia.mobile.discovery.core.domain.repository.CartRepository
import javax.inject.Inject

/**
 * Refreshes the basket against the store's current products (one `GET /api/v1/products`): prices,
 * availability and stock, removing the lines that can no longer be ordered. When the products cannot
 * be read, the basket is left untouched and the failure is returned.
 */
class RefreshCartUseCase @Inject constructor(
    private val pricingRepository: CartPricingRepository,
    private val cartRepository: CartRepository,
) {
    suspend operator fun invoke(): Result<CartRefresh> =
        pricingRepository.snapshots().map { snapshots -> cartRepository.refresh(snapshots) }
}
