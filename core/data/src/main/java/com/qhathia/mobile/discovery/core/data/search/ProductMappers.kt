package com.qhathia.mobile.discovery.core.data.search

import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductSearchResult
import com.qhathia.mobile.discovery.core.domain.model.SearchFilter
import com.qhathia.mobile.discovery.core.network.dto.ConversationSearchResponseDto
import com.qhathia.mobile.discovery.core.network.dto.SearchFilterDto
import com.qhathia.mobile.discovery.core.network.dto.SearchProductDto

/** Maps a search-result DTO to the domain [Product], keeping serialization concerns in `core:network`. */
internal fun SearchProductDto.toDomain(): Product = Product(
    id = id,
    name = name,
    description = description,
    price = price,
    availability = availability,
    categoryName = categoryName,
    imageUrl = primaryImageUrl.orEmpty(),
    discountedPrice = discountedPrice,
    discountEndsAt = discountEndsAt,
)

/** Maps a search-filter DTO to the domain [SearchFilter]. */
internal fun SearchFilterDto.toDomain(): SearchFilter = SearchFilter(
    criterion = criterion,
    options = options,
)

/** Maps the search response DTO to the domain [ProductSearchResult]. */
internal fun ConversationSearchResponseDto.toDomain(): ProductSearchResult {
    val products = results.map { it.toDomain() }
    return ProductSearchResult(
        products = products,
        displayText = displayText,
        understood = understood,
        noResults = noResults || products.isEmpty(),
        fallbackMessage = fallbackMessage,
        suggestedFromCategory = suggestedFromCategory,
        filters = filters.map { it.toDomain() },
        suggestions = suggestions,
    )
}
