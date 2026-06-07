package com.qhathia.mobile.discovery.core.domain.model

/**
 * Full detail of a single product, richer than the search-result [Product] (it carries the ordered
 * photo gallery and the full description). Framework-free domain model.
 *
 * @property id stable product identifier.
 * @property name display name.
 * @property description full description / characteristics.
 * @property price price in store currency (formatted for display by the UI layer).
 * @property availability availability status as returned by the API (e.g. `AVAILABLE`).
 * @property categoryName the product's category name, or empty when none.
 * @property images the product's photos, ordered primary-first then by position (may be empty).
 */
data class ProductDetail(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val availability: String,
    val categoryName: String,
    val images: List<ProductImage>,
)
