package com.qhathia.mobile.discovery.core.data.detail

import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductDetail
import com.qhathia.mobile.discovery.core.domain.model.ProductImage
import com.qhathia.mobile.discovery.core.network.dto.MediaDto
import com.qhathia.mobile.discovery.core.network.dto.ProductDetailResponseDto
import com.qhathia.mobile.discovery.core.network.dto.SuggestedProductDto

/** Maps a media DTO to the domain [ProductImage]. */
internal fun MediaDto.toDomain(): ProductImage = ProductImage(
    url = url,
    position = position,
    isPrimary = primary,
)

/**
 * Maps the product-detail response DTO to the domain [ProductDetail], keeping serialization concerns
 * in `core:network`. Only image media is carried into the gallery, ordered primary-first then by
 * position.
 */
internal fun ProductDetailResponseDto.toDomain(): ProductDetail = ProductDetail(
    id = id,
    name = name,
    description = description.orEmpty(),
    price = price,
    availability = availability,
    categoryName = categoryName.orEmpty(),
    images = media
        .filter { it.mediaType.equals("IMAGE", ignoreCase = true) }
        .sortedWith(compareByDescending(MediaDto::primary).thenBy(MediaDto::position))
        .map { it.toDomain() },
    discountedPrice = discountedPrice,
    discountEndsAt = discountEndsAt,
    active = active,
)

/**
 * Maps a suggested-product DTO (`ProductSummaryResponse`) to the domain [Product] used by the
 * related-products strip. No description is provided by this endpoint, so it maps to empty.
 */
internal fun SuggestedProductDto.toDomain(): Product = Product(
    id = id,
    name = name,
    description = "",
    price = price,
    availability = availability,
    categoryName = categoryName,
    imageUrl = primaryImageUrl.orEmpty(),
    allowDiscountInquiry = allowDiscountInquiry,
    discountedPrice = discountedPrice,
    discountEndsAt = discountEndsAt,
)
