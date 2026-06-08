package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.CartRepository
import javax.inject.Inject

/** Removes a product line from the cart. */
class RemoveCartItemUseCase @Inject constructor(
    private val repository: CartRepository,
) {
    suspend operator fun invoke(productId: String) = repository.removeProduct(productId)
}
