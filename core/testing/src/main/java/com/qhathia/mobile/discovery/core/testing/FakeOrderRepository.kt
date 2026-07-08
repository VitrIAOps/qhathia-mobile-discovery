package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.Order
import com.qhathia.mobile.discovery.core.domain.model.OrderErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.OrderException
import com.qhathia.mobile.discovery.core.domain.repository.OrderRepository

/**
 * Scriptable [OrderRepository] for use-case / ViewModel tests. Records the last call and returns a
 * pre-programmed [result]. Defaults to success with an order echoing the submitted lines.
 */
class FakeOrderRepository(
    var result: Result<Order>? = null,
) : OrderRepository {

    /** The arguments of the last [createOrder] call, or `null` if never called. */
    var lastCall: Call? = null
        private set

    /** Number of times [createOrder] was invoked. */
    var callCount: Int = 0
        private set

    data class Call(val name: String, val phone: String, val items: List<CartItem>)

    override suspend fun createOrder(
        name: String,
        phone: String,
        items: List<CartItem>,
    ): Result<Order> {
        callCount++
        lastCall = Call(name, phone, items)
        return result ?: Result.success(defaultOrder(name, phone, items))
    }

    /** Programs the next call to fail with [category]. */
    fun failWith(category: OrderErrorCategory = OrderErrorCategory.OrderApi) {
        result = Result.failure(OrderException(category))
    }

    private fun defaultOrder(name: String, phone: String, items: List<CartItem>): Order = Order(
        id = "order-1",
        status = "PENDING",
        subtotal = items.sumOf { it.lineSubtotal },
        total = items.sumOf { it.lineSubtotal },
        customerName = name,
        customerPhone = phone,
        items = items.map {
            com.qhathia.mobile.discovery.core.domain.model.OrderItem(
                productId = it.productId,
                productName = it.name,
                quantity = it.quantity,
                unitPriceSnapshot = it.priceUnit,
                lineTotal = it.lineSubtotal,
            )
        },
    )
}
