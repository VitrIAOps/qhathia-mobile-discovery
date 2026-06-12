package com.qhathia.mobile.discovery.core.domain.model

/**
 * A product returned by a search. Framework-free domain model.
 *
 * @property id stable product identifier.
 * @property name display name.
 * @property description short description.
 * @property price price in store currency (formatted for display by the UI layer).
 * @property availability availability status as returned by the API (e.g. `AVAILABLE`).
 * @property categoryName the product's category name, or empty when none.
 * @property imageUrl primary image URL, or empty when none.
 * @property allowDiscountInquiry whether the consumer may ask about discounts for this product. Drives
 *   the catalog's synthetic "Descuentos" section. Defaults to `false` for sources that don't expose it
 *   (e.g. conversational search results), where it means "unknown / none".
 */
data class Product(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val availability: String,
    val categoryName: String,
    val imageUrl: String,
    val allowDiscountInquiry: Boolean = false,
)
