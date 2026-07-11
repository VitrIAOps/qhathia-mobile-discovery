package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.CartShare
import com.qhathia.mobile.discovery.core.domain.repository.CartShareRepository
import javax.inject.Inject

/**
 * Shares the cart basket as a WhatsApp quote for the given consumer data. Thin delegation over
 * [CartShareRepository]; field validation happens upstream (the cart form) so an invalid basket never
 * reaches here.
 */
class ShareCartUseCase @Inject constructor(
    private val repository: CartShareRepository,
) {
    suspend operator fun invoke(name: String, phone: String, items: List<CartItem>): Result<CartShare> =
        repository.shareCart(name, phone, items)
}
