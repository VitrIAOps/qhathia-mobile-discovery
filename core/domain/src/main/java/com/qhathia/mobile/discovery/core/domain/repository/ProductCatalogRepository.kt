package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.CatalogCategory

/**
 * Port for the store catalog. Implemented in `core:data`, which calls the Qhathia catalog API and maps
 * the result to a list of [CatalogCategory]; failures surface as a failed [Result] carrying a
 * `CatalogException` so callers can choose the recovery UX.
 */
interface ProductCatalogRepository {

    /**
     * Loads the store's available products grouped by category. Returns the categories (already
     * filtered and ordered server-side), or a failure tagged with a `CatalogException`.
     */
    suspend fun getCatalog(): Result<List<CatalogCategory>>
}
