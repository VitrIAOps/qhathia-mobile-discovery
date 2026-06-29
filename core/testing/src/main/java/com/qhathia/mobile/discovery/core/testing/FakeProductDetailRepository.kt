package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductDetail
import com.qhathia.mobile.discovery.core.domain.repository.ProductDetailRepository
import kotlinx.coroutines.delay

/**
 * Configurable [ProductDetailRepository] for tests. [result] is returned for every detail fetch and
 * [suggestionsResult] for every suggestions fetch; [delayMillis] simulates a slow request so loading
 * states can be exercised on virtual time.
 */
class FakeProductDetailRepository(
    var result: Result<ProductDetail> = Result.success(ProductDetailFixtures.productDetail()),
    var suggestionsResult: Result<List<Product>> = Result.success(ProductDetailFixtures.suggestions()),
    var delayMillis: Long = 0,
) : ProductDetailRepository {

    val requestedIds: MutableList<String> = mutableListOf()
    val suggestionRequestedIds: MutableList<String> = mutableListOf()
    var callCount: Int = 0
        private set

    override suspend fun getProduct(id: String): Result<ProductDetail> {
        requestedIds += id
        callCount++
        if (delayMillis > 0) delay(delayMillis)
        return result
    }

    override suspend fun getSuggestions(id: String): Result<List<Product>> {
        suggestionRequestedIds += id
        if (delayMillis > 0) delay(delayMillis)
        return suggestionsResult
    }
}
