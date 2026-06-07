package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.ProductDetail
import com.qhathia.mobile.discovery.core.domain.repository.ProductDetailRepository
import kotlinx.coroutines.delay

/**
 * Configurable [ProductDetailRepository] for tests. [result] is returned for every fetch; [delayMillis]
 * simulates a slow request so loading states can be exercised on virtual time.
 */
class FakeProductDetailRepository(
    var result: Result<ProductDetail> = Result.success(ProductDetailFixtures.productDetail()),
    var delayMillis: Long = 0,
) : ProductDetailRepository {

    val requestedIds: MutableList<String> = mutableListOf()
    var callCount: Int = 0
        private set

    override suspend fun getProduct(id: String): Result<ProductDetail> {
        requestedIds += id
        callCount++
        if (delayMillis > 0) delay(delayMillis)
        return result
    }
}
