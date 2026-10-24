package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductSearchResult
import com.qhathia.mobile.discovery.core.domain.model.SearchFilter

/** Canonical product-search test data so tests don't re-declare product/result shapes. */
object ProductFixtures {

    fun filters(): List<SearchFilter> = listOf(
        SearchFilter(criterion = "Marca", options = listOf("Bosch", "Makita", "Dewalt")),
        SearchFilter(criterion = "Voltaje", options = listOf("12V", "18V", "20V")),
    )

    fun suggestions(): List<String> = listOf("Para uso en casa", "Para uso profesional", "Percutor")

    fun product(
        id: String = "p1",
        name: String = "Taladro Percutor Bosch GSB 13 RE Professional",
        description: String = "Taladro percutor profesional 650W",
        price: Double = 380.0,
        availability: String = "AVAILABLE",
        categoryName: String = "Taladros",
        imageUrl: String = "https://example.com/taladro.jpg",
        allowDiscountInquiry: Boolean = false,
        discountedPrice: Double? = null,
        discountEndsAt: String? = null,
    ): Product = Product(
        id = id,
        name = name,
        description = description,
        price = price,
        availability = availability,
        categoryName = categoryName,
        imageUrl = imageUrl,
        allowDiscountInquiry = allowDiscountInquiry,
        discountedPrice = discountedPrice,
        discountEndsAt = discountEndsAt,
    )

    fun products(count: Int): List<Product> =
        (1..count).map { product(id = "p$it", name = "Producto $it") }

    fun result(
        products: List<Product> = products(8),
        displayText: String = "Encontré ${products.size} taladros para ti",
        understood: Boolean = true,
        noResults: Boolean = false,
        fallbackMessage: String? = null,
        suggestedFromCategory: String? = null,
        filters: List<SearchFilter> = filters(),
        suggestions: List<String> = suggestions(),
    ): ProductSearchResult = ProductSearchResult(
        products = products,
        displayText = displayText,
        understood = understood,
        noResults = noResults,
        fallbackMessage = fallbackMessage,
        suggestedFromCategory = suggestedFromCategory,
        filters = filters,
        suggestions = suggestions,
    )

    /**
     * Unhappy-path result: the backend returns `noResults=true` with up to 3 store suggestions in
     * [products] plus the [suggestedFromCategory] they were drawn from. Defaults mirror the Fase 8
     * contract; pass `products = emptyList()` to model a store with no AVAILABLE suggestions.
     */
    fun noResults(
        products: List<Product> = products(3),
        suggestedFromCategory: String? = "taladro",
        fallbackMessage: String? = "No encontré productos para tu búsqueda.",
    ): ProductSearchResult = ProductSearchResult(
        products = products,
        displayText = "",
        understood = true,
        noResults = true,
        fallbackMessage = fallbackMessage,
        suggestedFromCategory = suggestedFromCategory,
        filters = emptyList(),
        suggestions = emptyList(),
    )
}
