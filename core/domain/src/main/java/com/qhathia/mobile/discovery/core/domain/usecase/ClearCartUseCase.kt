package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.CartRepository
import javax.inject.Inject

/**
 * Empties the cart. Invoked when the consumer places an order and by the off-Home inactivity timeout.
 */
class ClearCartUseCase @Inject constructor(
    private val repository: CartRepository,
) {
    suspend operator fun invoke() = repository.clear()
}
