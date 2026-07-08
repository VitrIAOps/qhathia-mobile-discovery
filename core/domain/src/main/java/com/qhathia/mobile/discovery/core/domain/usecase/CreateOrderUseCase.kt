package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.Order
import com.qhathia.mobile.discovery.core.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Creates a purchase order from the cart basket and consumer data. Thin delegation over
 * [OrderRepository]; field validation happens upstream (the cart form) so an invalid basket never
 * reaches here.
 */
class CreateOrderUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    suspend operator fun invoke(name: String, phone: String, items: List<CartItem>): Result<Order> =
        repository.createOrder(name, phone, items)
}
