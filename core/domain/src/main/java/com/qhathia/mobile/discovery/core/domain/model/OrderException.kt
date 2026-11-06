package com.qhathia.mobile.discovery.core.domain.model

/**
 * Failure carried by a failed order-creation [Result]: its [category] and, when the server named
 * them, the [details] (changed prices, the product, the units left, the rejected fields).
 */
class OrderException(
    val category: OrderErrorCategory,
    val details: OrderFailureDetails = OrderFailureDetails(),
) : Exception("Order creation failed: $category")
