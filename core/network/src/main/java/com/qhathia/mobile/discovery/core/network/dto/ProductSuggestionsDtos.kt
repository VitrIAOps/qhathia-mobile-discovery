package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of `GET /api/v1/products/{id}/suggestions` (Fase 10). [suggestions] holds 0..5 complementary
 * products for the base product; [source] is informational (which cascading path produced the result)
 * and is not consumed by the client. Defaults keep deserialization resilient and `ignoreUnknownKeys`
 * drops unmodeled fields (e.g. `position`, `tags`).
 */
@Serializable
data class ProductSuggestionsResponseDto(
    val productId: String = "",
    val suggestions: List<SuggestedProductDto> = emptyList(),
    val source: String = "",
)

/**
 * A suggested product (`ProductSummaryResponse` — the same shape as `/products` and
 * `/catalog/by-category`). Only the fields the related-products strip renders are modeled;
 * `primaryImageUrl` and `brand` are nullable.
 * `discountedPrice` is the server-computed price with the active discount applied (`null` = no active
 * discount) and `discountEndsAt` its ISO-8601 end (Fase 25).
 */
@Serializable
data class SuggestedProductDto(
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
