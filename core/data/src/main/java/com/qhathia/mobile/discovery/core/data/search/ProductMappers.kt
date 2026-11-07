package com.qhathia.mobile.discovery.core.data.search

import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductSearchResult
import com.qhathia.mobile.discovery.core.domain.model.SearchFilter
import com.qhathia.mobile.discovery.core.domain.model.StructuredSearchFilters
import com.qhathia.mobile.discovery.core.network.dto.ConversationSearchResponseDto
import com.qhathia.mobile.discovery.core.network.dto.SearchFilterDto
import com.qhathia.mobile.discovery.core.network.dto.SearchFiltersDto
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
    stock = stock,
)

/** Maps a search-filter DTO to the domain [SearchFilter]. */
internal fun SearchFilterDto.toDomain(): SearchFilter = SearchFilter(
    criterion = criterion,
    options = options,
    values = values.ifEmpty { options },
)

/** Maps the domain filters to the request DTO; an empty list is not sent. */
internal fun StructuredSearchFilters.toDto(): SearchFiltersDto = SearchFiltersDto(
    brands = brands.takeIf { it.isNotEmpty() },
    tags = tags.takeIf { it.isNotEmpty() },
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
        alternativesOnly = alternativesOnly,
        missingCategory = missingCategory,
    )
}
