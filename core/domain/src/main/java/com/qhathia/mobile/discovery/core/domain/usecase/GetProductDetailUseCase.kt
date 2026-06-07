package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.ProductDetail
import com.qhathia.mobile.discovery.core.domain.repository.ProductDetailRepository
import javax.inject.Inject

/**
 * Fetches the detail of a single product by id, delegating to the [ProductDetailRepository].
 */
class GetProductDetailUseCase @Inject constructor(
    private val repository: ProductDetailRepository,
) {
    suspend operator fun invoke(id: String): Result<ProductDetail> = repository.getProduct(id)
}
