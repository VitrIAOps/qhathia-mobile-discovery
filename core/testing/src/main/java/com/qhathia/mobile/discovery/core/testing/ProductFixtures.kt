package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductSearchResult

/** Canonical product-search test data so tests don't re-declare product/result shapes. */
object ProductFixtures {

    fun product(
        id: String = "p1",
        name: String = "Taladro Percutor Bosch GSB 13 RE Professional",
        description: String = "Taladro percutor profesional 650W",
        price: Double = 380.0,
        availability: String = "AVAILABLE",
        categoryName: String = "Taladros",
        imageUrl: String = "https://example.com/taladro.jpg",
    ): Product = Product(
        id = id,
        name = name,
        description = description,
        price = price,
        availability = availability,
        categoryName = categoryName,
        imageUrl = imageUrl,
    )

    fun products(count: Int): List<Product> =
        (1..count).map { product(id = "p$it", name = "Producto $it") }

    fun result(
        products: List<Product> = products(8),
        displayText: String = "Encontré ${products.size} taladros para ti",
        understood: Boolean = true,
        noResults: Boolean = false,
        fallbackMessage: String? = null,
    ): ProductSearchResult = ProductSearchResult(
        products = products,
        displayText = displayText,
        understood = understood,
        noResults = noResults,
        fallbackMessage = fallbackMessage,
    )

    fun noResults(): ProductSearchResult = ProductSearchResult(
        products = emptyList(),
        displayText = "",
        understood = true,
        noResults = true,
        fallbackMessage = "No encontré productos para tu búsqueda.",
    )
}
