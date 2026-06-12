package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.CatalogCategory
import com.qhathia.mobile.discovery.core.domain.repository.ProductCatalogRepository
import javax.inject.Inject

/** Loads the store's catalog grouped by category for the catalog screen. */
class GetProductCatalogUseCase @Inject constructor(
    private val repository: ProductCatalogRepository,
) {
    suspend operator fun invoke(): Result<List<CatalogCategory>> = repository.getCatalog()
}
