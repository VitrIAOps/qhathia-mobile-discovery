package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.CartRepository
import javax.inject.Inject

/**
 * Sets a cart line's quantity. The quantity is capped at the product's stock max; a value below 1
 * removes the line (enforced by the [com.qhathia.mobile.discovery.core.domain.model.Cart] transform).
 */
class UpdateCartItemQuantityUseCase @Inject constructor(
    private val repository: CartRepository,
) {
    suspend operator fun invoke(productId: String, quantity: Int) =
        repository.setQuantity(productId, quantity)
}
