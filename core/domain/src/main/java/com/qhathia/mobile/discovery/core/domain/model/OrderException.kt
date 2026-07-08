package com.qhathia.mobile.discovery.core.domain.model

/**
 * Failure carried by a failed order-creation [Result], tagging it with the [OrderErrorCategory] so the
 * presentation layer stays free of low-level causes. The cart collapses every category to the same
 * simplified message.
 */
class OrderException(val category: OrderErrorCategory) :
    Exception("Order creation failed: $category")
