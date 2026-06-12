package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.CatalogCategory
import com.qhathia.mobile.discovery.core.domain.repository.ProductCatalogRepository

/**
 * Configurable [ProductCatalogRepository] for tests. [result] is returned for every load; tests can
 * flip it to drive the success / empty / error paths. [callCount] tracks invocations (e.g. for retry).
 */
class FakeProductCatalogRepository(
    var result: Result<List<CatalogCategory>> = Result.success(CatalogFixtures.catalog()),
) : ProductCatalogRepository {

    var callCount: Int = 0
        private set

    override suspend fun getCatalog(): Result<List<CatalogCategory>> {
        callCount++
        return result
    }
}
