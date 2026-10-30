package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.CartRepository
import com.qhathia.mobile.discovery.core.domain.repository.CustomerSessionRepository
import javax.inject.Inject

/**
 * Ends the current customer's session: clears the basket, then signals the end so the cart form and
 * the product share form reset. The single path for "Aceptar" on a success screen, "Cancelar orden"
 * on an error screen, and the inactivity timeout, so all three clear the same data.
 */
class EndCustomerSessionUseCase @Inject constructor(
    private val cartRepository: CartRepository,
    private val customerSessionRepository: CustomerSessionRepository,
) {
    suspend operator fun invoke() {
        cartRepository.clear()
        customerSessionRepository.end()
    }
}
