package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.CatalogCategory
import com.qhathia.mobile.discovery.core.domain.model.Product

/** Canonical catalog test data so tests don't re-declare category/product shapes. */
object CatalogFixtures {

    fun category(
        id: String = "c1",
        name: String = "Iluminación",
        products: List<Product> = listOf(ProductFixtures.product()),
    ): CatalogCategory = CatalogCategory(id = id, name = name, products = products)

    /**
     * Two categories; [discounted] controls how many products across the catalog have
     * `allowDiscountInquiry == true` (used to exercise the Descuentos section).
     */
    fun catalog(discounted: Int = 2): List<CatalogCategory> {
        val lighting = (1..3).map {
            ProductFixtures.product(id = "l$it", name = "Lámpara $it", allowDiscountInquiry = it <= discounted)
        }
        val electric = (1..2).map {
            ProductFixtures.product(id = "e$it", name = "Interruptor $it", categoryName = "Eléctrico")
        }
        return listOf(
            category(id = "c1", name = "Iluminación", products = lighting),
            category(id = "c2", name = "Eléctrico", products = electric),
        )
    }
}
