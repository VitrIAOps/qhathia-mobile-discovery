package com.qhathia.mobile.discovery.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of `GET /api/v1/products/{id}` (the full `ProductResponse`). Only the fields the client
 * renders are modeled; `ignoreUnknownKeys` drops the rest (e.g. `storeId`, `categoryId`, `tags`,
 * `position`, `active`, timestamps). `discounts[]` is kept for reference only; the price to show is
 * `discountedPrice`, the server-computed price with the active discount applied (`null` = no active
 * discount) and `discountEndsAt` its ISO-8601 end (Fase 25).
 */
@Serializable
data class ProductDetailResponseDto(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val price: Double = 0.0,
    val availability: String = "",
    val categoryName: String? = null,
    val media: List<MediaDto> = emptyList(),
    val discounts: List<DiscountDto> = emptyList(),
    val discountedPrice: Double? = null,
    val discountEndsAt: String? = null,
)

@Serializable
data class MediaDto(
    val id: String = "",
    val url: String = "",
    @SerialName("mediaType") val mediaType: String = "IMAGE",
    val position: Int = 0,
    val primary: Boolean = false,
)

@Serializable
data class DiscountDto(
    val id: String = "",
    @SerialName("discountType") val discountType: String = "",
    val value: Double = 0.0,
    val active: Boolean = false,
)
