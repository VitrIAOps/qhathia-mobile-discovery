package com.qhathia.mobile.discovery.feature.productdetail.data

import com.qhathia.mobile.discovery.core.domain.model.Product

/**
 * PLACEHOLDER — temporary mock for the "Podrías acompañar este producto con…" section.
 *
 * The related-products API is not built yet. This mock lets the UI render the section now; replace it
 * with a real domain use case / repository call once `GET` related products is available. Do NOT ship
 * this as the long-term source of related products.
 *
 * TODO(related-products-api): remove this mock and source related products from the backend.
 */
internal object RelatedProductsMock {

    fun forProduct(productId: String): List<Product> = listOf(
        Product(
            id = "$productId-rel-1",
            name = "Set de brocas para concreto (5 piezas)",
            description = "",
            price = 49.90,
            availability = "AVAILABLE",
            categoryName = "Accesorios",
            imageUrl = "",
        ),
        Product(
            id = "$productId-rel-2",
            name = "Lentes de seguridad antiempañantes",
            description = "",
            price = 19.90,
            availability = "AVAILABLE",
            categoryName = "Seguridad",
            imageUrl = "",
        ),
        Product(
            id = "$productId-rel-3",
            name = "Guantes de trabajo reforzados",
            description = "",
            price = 24.90,
            availability = "AVAILABLE",
            categoryName = "Seguridad",
            imageUrl = "",
        ),
    )
}
