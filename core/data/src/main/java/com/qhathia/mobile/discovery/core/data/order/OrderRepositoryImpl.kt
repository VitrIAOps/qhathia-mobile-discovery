package com.qhathia.mobile.discovery.core.data.order

import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.Order
import com.qhathia.mobile.discovery.core.domain.model.OrderErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.OrderException
import com.qhathia.mobile.discovery.core.domain.repository.OrderRepository
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.OrderApiService
import javax.inject.Inject

/**
 * Creates a purchase order via `POST /api/v1/orders` and maps the result to a domain [Order].
 *
 * Obtains the DEVICE session JWT via the shared [SessionTokenProvider] (the interim auth seam reused
 * from the STT/detail pipeline). On a `401` the cached token is dropped and the call is retried once
 * with a fresh login. Failures surface as a failed [Result] carrying an [OrderException] tagged with
 * the matching [OrderErrorCategory]; the cart collapses every category to one simplified message.
 */
class OrderRepositoryImpl @Inject constructor(
    private val sessionTokenProvider: SessionTokenProvider,
    private val orderApiService: OrderApiService,
) : OrderRepository {

    override suspend fun createOrder(
        name: String,
        phone: String,
        items: List<CartItem>,
    ): Result<Order> {
        val request = buildCreateOrderRequest(name, phone, items)

        val token = sessionTokenProvider.accessToken()
            .getOrElse { return failure(OrderErrorMapper.fromTokenFailure(it)) }

        var response = orderApiService.createOrder(token, request)
        if (response is NetworkResult.HttpError && response.code == HTTP_UNAUTHORIZED) {
            sessionTokenProvider.clear()
            val refreshed = sessionTokenProvider.accessToken(forceRefresh = true)
                .getOrElse { return failure(OrderErrorMapper.fromTokenFailure(it)) }
            response = orderApiService.createOrder(refreshed, request)
        }

        return when (response) {
            is NetworkResult.Success -> Result.success(response.data.toDomain())
            is NetworkResult.HttpError -> failure(OrderErrorCategory.OrderApi)
            is NetworkResult.NetworkError -> failure(OrderErrorMapper.fromNetworkError(response.kind))
        }
    }

    private fun failure(category: OrderErrorCategory): Result<Order> =
        Result.failure(OrderException(category))

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
