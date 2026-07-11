package com.qhathia.mobile.discovery.core.domain.model

/**
 * Failure carried by a failed cart-share [Result], tagging it with the [CartShareErrorCategory] so the
 * presentation layer stays free of low-level causes. The cart collapses every category to the same
 * simplified message.
 */
class CartShareException(val category: CartShareErrorCategory) :
    Exception("Cart share failed: $category")
