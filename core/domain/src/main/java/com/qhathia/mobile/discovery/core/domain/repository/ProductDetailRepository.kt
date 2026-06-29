package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductDetail

/**
 * Port for fetching a single product's detail and its complementary suggestions. Implemented in
 * `core:data`, which calls the Qhathia product API and maps results to domain models; failures surface
 * as a failed [Result] carrying a `DetailException` so callers can choose the recovery UX.
 */
interface ProductDetailRepository {

    /** Fetches the detail of the product identified by [id], or a failure tagged with a `DetailException`. */
    suspend fun getProduct(id: String): Result<ProductDetail>

    /**
     * Fetches up to 5 complementary products for the product identified by [id]
     * (`GET /api/v1/products/{id}/suggestions`), or a failure tagged with a `DetailException`. An empty
     * list is a valid success (the store has no other products to suggest).
     */
    suspend fun getSuggestions(id: String): Result<List<Product>>
}
