package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.ProductSearchConstants
import com.qhathia.mobile.discovery.core.domain.model.ProductSearchResult
import com.qhathia.mobile.discovery.core.domain.repository.ProductSearchRepository
import javax.inject.Inject

/**
 * Searches for products matching a query. Enforces the business cap of
 * [ProductSearchConstants.MAX_QUERY_CHARS] characters (trimming and truncating the input) before
 * delegating to the [ProductSearchRepository].
 */
class SearchProductsUseCase @Inject constructor(
    private val repository: ProductSearchRepository,
) {
    suspend operator fun invoke(query: String, language: String = "es"): Result<ProductSearchResult> {
        val capped = query.trim().take(ProductSearchConstants.MAX_QUERY_CHARS)
        return repository.search(capped, language)
    }
}
