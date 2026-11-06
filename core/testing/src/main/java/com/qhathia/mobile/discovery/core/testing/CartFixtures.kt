package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.CartConstants
import com.qhathia.mobile.discovery.core.domain.model.Cart
import com.qhathia.mobile.discovery.core.domain.model.CartItem

/** Canonical cart test data so tests don't re-declare cart-item/cart shapes. */
object CartFixtures {

    fun cartItem(
        productId: String = "p1",
        name: String = "Taladro Percutor Bosch GSB 13 RE Professional",
        priceUnit: Double = 189.0,
        imageUrl: String = "https://example.com/taladro.jpg",
        availability: String = "AVAILABLE",
        quantity: Int = 1,
        stock: Int? = null,
        maxQuantity: Int = CartConstants.maxQuantityFor(availability, stock),
    ): CartItem = CartItem(
        productId = productId,
        name = name,
        priceUnit = priceUnit,
        imageUrl = imageUrl,
        availability = availability,
        quantity = quantity,
        maxQuantity = maxQuantity,
        stock = stock,
    )

    fun cart(
        items: List<CartItem> = listOf(
            cartItem(productId = "p1", name = "Taladro", priceUnit = 189.0, quantity = 1),
            cartItem(productId = "p2", name = "Set de brocas", priceUnit = 49.9, quantity = 2),
        ),
    ): Cart = Cart(items = items)
}
