package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.Order

/**
 * Port for creating a purchase order from the cart. Implemented in `core:data` against
 * `POST /api/v1/orders`. The consumer name/phone come from the cart form and the lines from the
 * basket; the server owns the totals.
 */
interface OrderRepository {

    /**
     * Creates an order for [items] with the consumer's [name] and [phone]. Returns the created [Order]
     * on success, or a failure carrying an
     * [com.qhathia.mobile.discovery.core.domain.model.OrderException].
     */
    suspend fun createOrder(name: String, phone: String, items: List<CartItem>): Result<Order>
}
