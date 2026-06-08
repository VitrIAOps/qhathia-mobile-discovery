package com.qhathia.mobile.discovery.core.domain.model

import com.qhathia.mobile.discovery.core.domain.CartConstants

/**
 * Builds a [CartItem] from a search-result [Product] with the given [quantity]. The stock cap is
 * derived from the product's availability (see [CartConstants.maxQuantityFor]).
 */
fun Product.toCartItem(quantity: Int = 1): CartItem = CartItem(
    productId = id,
    name = name,
    priceUnit = price,
    imageUrl = imageUrl,
    availability = availability,
    quantity = quantity,
    maxQuantity = CartConstants.maxQuantityFor(availability),
)

/**
 * Builds a [CartItem] from a [ProductDetail] with the given [quantity]. Uses the first (primary)
 * gallery image and derives the stock cap from the product's availability.
 */
fun ProductDetail.toCartItem(quantity: Int = 1): CartItem = CartItem(
    productId = id,
    name = name,
    priceUnit = price,
    imageUrl = images.firstOrNull()?.url.orEmpty(),
    availability = availability,
    quantity = quantity,
    maxQuantity = CartConstants.maxQuantityFor(availability),
)
