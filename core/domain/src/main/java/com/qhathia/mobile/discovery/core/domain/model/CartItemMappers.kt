package com.qhathia.mobile.discovery.core.domain.model

import com.qhathia.mobile.discovery.core.domain.CartConstants

/**
 * Builds a [CartItem] from a search-result [Product] with the given [quantity]. The unit price is the
 * one the customer pays: the server-computed [Product.discountedPrice] when there is an active
 * discount, else the list price. The quantity cap is the product's stock when known, else derived from
 * its availability (see [CartConstants.maxQuantityFor]).
 */
fun Product.toCartItem(quantity: Int = 1): CartItem = CartItem(
    productId = id,
    name = name,
    priceUnit = discountedPrice ?: price,
    imageUrl = imageUrl,
    availability = availability,
    quantity = quantity,
    maxQuantity = CartConstants.maxQuantityFor(availability, stock),
    stock = stock,
)

/**
 * Builds a [CartItem] from a [ProductDetail] with the given [quantity]. Like [Product.toCartItem], the
 * unit price is [ProductDetail.discountedPrice] when set, else the list price. Uses the first (primary)
 * gallery image and caps the quantity like [Product.toCartItem].
 */
fun ProductDetail.toCartItem(quantity: Int = 1): CartItem = CartItem(
    productId = id,
    name = name,
    priceUnit = discountedPrice ?: price,
    imageUrl = images.firstOrNull()?.url.orEmpty(),
    availability = availability,
    quantity = quantity,
    maxQuantity = CartConstants.maxQuantityFor(availability, stock),
    stock = stock,
)
