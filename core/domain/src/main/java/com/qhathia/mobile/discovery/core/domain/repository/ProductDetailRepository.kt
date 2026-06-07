package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.ProductDetail

/**
 * Port for fetching a single product's detail. Implemented in `core:data`, which calls the Qhathia
 * product API and maps the result to a [ProductDetail]; failures surface as a failed [Result] carrying
 * a `DetailException` so callers can choose the recovery UX.
 */
interface ProductDetailRepository {

    /** Fetches the detail of the product identified by [id], or a failure tagged with a `DetailException`. */
    suspend fun getProduct(id: String): Result<ProductDetail>
}
