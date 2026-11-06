package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductDetail
import com.qhathia.mobile.discovery.core.domain.model.ProductImage

/** Canonical product-detail test data so tests don't re-declare detail/image shapes. */
object ProductDetailFixtures {

    /** Sample related products (the "Podrías acompañar este producto con…" strip). */
    fun suggestions(count: Int = 3): List<Product> = (1..count).map {
        Product(
            id = "rel$it",
            name = "Producto relacionado $it",
            description = "",
            price = 19.90 * it,
            availability = "AVAILABLE",
            categoryName = "Accesorios",
            imageUrl = "https://example.com/rel-$it.jpg",
        )
    }

    fun image(
        url: String = "https://example.com/taladro-1.jpg",
        position: Int = 0,
        isPrimary: Boolean = position == 0,
    ): ProductImage = ProductImage(url = url, position = position, isPrimary = isPrimary)

    /** Builds [count] images (the first one primary), or an empty list when [count] is 0. */
    fun images(count: Int): List<ProductImage> =
        (0 until count).map { image(url = "https://example.com/taladro-$it.jpg", position = it) }

    fun productDetail(
        id: String = "p1",
        name: String = "Taladro Percutor Bosch GSB 13 RE Professional",
        description: String = "Taladro percutor profesional 650W, ideal para concreto y madera.",
        price: Double = 380.0,
        availability: String = "AVAILABLE",
        categoryName: String = "Taladros",
        images: List<ProductImage> = images(3),
        active: Boolean? = true,
        stock: Int? = null,
    ): ProductDetail = ProductDetail(
        id = id,
        name = name,
        description = description,
        price = price,
        availability = availability,
        categoryName = categoryName,
        images = images,
        active = active,
        stock = stock,
    )
}
