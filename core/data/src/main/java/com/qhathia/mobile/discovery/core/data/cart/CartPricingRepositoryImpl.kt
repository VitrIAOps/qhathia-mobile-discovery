package com.qhathia.mobile.discovery.core.data.cart

import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.ProductSnapshot
import com.qhathia.mobile.discovery.core.domain.repository.CartPricingRepository
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.ProductApiService
import javax.inject.Inject

/**
 * [CartPricingRepository] over `GET /api/v1/products`: one request for the whole store, which does
 * not count product views. The unit price is the one the customer pays now (`discountedPrice` when
 * set, else `price`), the same rule the server applies to orders.
 */
class CartPricingRepositoryImpl @Inject constructor(
    private val sessionTokenProvider: SessionTokenProvider,
    private val productApiService: ProductApiService,
) : CartPricingRepository {

    override suspend fun snapshots(): Result<Map<String, ProductSnapshot>> {
        val token = sessionTokenProvider.accessToken().getOrElse { return Result.failure(it) }
        return when (val response = productApiService.listProducts(token)) {
            is NetworkResult.Success -> Result.success(
                response.data.associate { dto ->
                    dto.id to ProductSnapshot(
                        id = dto.id,
                        unitPrice = dto.discountedPrice ?: dto.price,
                        availability = dto.availability,
                        stock = dto.stock,
                    )
                },
            )
            is NetworkResult.HttpError -> Result.failure(IllegalStateException("HTTP ${response.code}"))
            is NetworkResult.NetworkError -> Result.failure(response.cause)
        }
    }
}
