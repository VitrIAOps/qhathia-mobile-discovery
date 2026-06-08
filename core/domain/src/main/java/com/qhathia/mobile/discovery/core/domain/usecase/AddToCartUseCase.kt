package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.repository.CartRepository
import javax.inject.Inject

/**
 * Adds a product to the cart. Adding the same product again merges into its existing line; the
 * quantity is bounded by the product's stock cap (enforced by the [com.qhathia.mobile.discovery.core.domain.model.Cart]
 * transform the repository applies).
 */
class AddToCartUseCase @Inject constructor(
    private val repository: CartRepository,
) {
    suspend operator fun invoke(item: CartItem) = repository.addProduct(item)
}
