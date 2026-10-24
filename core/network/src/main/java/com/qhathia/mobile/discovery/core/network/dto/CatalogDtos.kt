package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One category with its embedded available products, as returned by `GET /api/v1/catalog/by-category`.
 * Only the fields the client renders are modeled; `ignoreUnknownKeys` drops the rest (e.g.
 * `categorySlug`, `position`). Defaults keep deserialization resilient to missing fields.
 */
@Serializable
data class CatalogCategoryDto(
    val categoryId: String = "",
    val categoryName: String = "",
    val products: List<CatalogProductDto> = emptyList(),
)

/**
 * A product summary inside a catalog category (`ProductSummaryResponse`). `availability` is always
 * `"AVAILABLE"` for this endpoint; `primaryImageUrl` and `brand` are nullable. `discountedPrice` is
 * the server-computed price with the active discount applied (`null` = no active discount) and
 * `discountEndsAt` is that discount's ISO-8601 end (Fase 25).
 */
@Serializable
data class CatalogProductDto(
    val id: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val availability: String = "",
    val allowDiscountInquiry: Boolean = false,
    val discountedPrice: Double? = null,
    val discountEndsAt: String? = null,
    @SerialName("primaryImageUrl") val primaryImageUrl: String? = null,
    val brand: String? = null,
)
