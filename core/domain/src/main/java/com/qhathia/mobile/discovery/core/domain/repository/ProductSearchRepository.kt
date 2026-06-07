package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.ProductSearchResult

/**
 * Port for product search. Implemented in `core:data`, which calls the Qhathia search API and maps
 * the result to a [ProductSearchResult]; failures surface as a failed [Result] carrying a
 * `SearchException` so callers can choose the recovery UX.
 */
interface ProductSearchRepository {

    /**
     * Searches for products matching [query] in the given [language] (ISO 639-1).
     * Returns the matching products, or a failure tagged with a `SearchException`.
     */
    suspend fun search(query: String, language: String = "es"): Result<ProductSearchResult>
}
