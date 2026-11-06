package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.CartPriceChange
import com.qhathia.mobile.discovery.core.domain.repository.CartRepository
import javax.inject.Inject

/** Applies the new unit prices of a `409 PRICE_CHANGED` to the basket. */
class UpdateCartPricesUseCase @Inject constructor(
    private val cartRepository: CartRepository,
) {
    suspend operator fun invoke(changes: List<CartPriceChange>) = cartRepository.updatePrices(changes)
}
