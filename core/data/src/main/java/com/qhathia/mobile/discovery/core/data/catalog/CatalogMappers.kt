package com.qhathia.mobile.discovery.core.data.catalog

import com.qhathia.mobile.discovery.core.domain.model.CatalogCategory
import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.network.dto.CatalogCategoryDto
import com.qhathia.mobile.discovery.core.network.dto.CatalogProductDto

/** Maps a catalog product DTO to the domain [Product], carrying the server-computed discount fields. */
internal fun CatalogProductDto.toDomain(): Product = Product(
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

/** Maps a catalog category DTO to the domain [CatalogCategory]. */
internal fun CatalogCategoryDto.toDomain(): CatalogCategory = CatalogCategory(
    id = categoryId,
    name = categoryName,
    products = products.map { it.toDomain() },
)
