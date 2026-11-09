package com.qhathia.mobile.discovery.core.data.order

import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.Order
import com.qhathia.mobile.discovery.core.domain.model.OrderErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.OrderException
import com.qhathia.mobile.discovery.core.domain.repository.CustomerSessionRepository
import com.qhathia.mobile.discovery.core.domain.repository.OrderRepository
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.OrderApiService
import javax.inject.Inject

/**
 * Creates a purchase order via `POST /api/v1/orders` and maps the result to a domain [Order].
 *
 * Obtains the DEVICE session JWT via the shared [SessionTokenProvider]. A `401` is not retried: an unlinked tablet ends its session through the HTTP client's unlink signal.
 * Failures surface as a failed [Result] carrying an [OrderException] tagged with
 * the matching [OrderErrorCategory] and, when the server named them, the failure details.
 */
class OrderRepositoryImpl @Inject constructor(
    private val sessionTokenProvider: SessionTokenProvider,
    private val orderApiService: OrderApiService,
    private val customerSession: CustomerSessionRepository,
) : OrderRepository {

    override suspend fun createOrder(
        name: String,
        phone: String,
        items: List<CartItem>,
    ): Result<Order> {
        val request = buildCreateOrderRequest(name, phone, items, sessionId = customerSession.currentSessionId())

        val token = sessionTokenProvider.accessToken()
            .getOrElse { return failure(OrderErrorMapper.fromTokenFailure(it)) }

        val response = orderApiService.createOrder(token, request)

        return when (response) {
            is NetworkResult.Success -> Result.success(response.data.toDomain())
            is NetworkResult.HttpError -> Result.failure(OrderErrorMapper.fromHttpError(response.code, response.problem))
            is NetworkResult.NetworkError -> failure(OrderErrorMapper.fromNetworkError(response.kind))
        }
    }

    private fun failure(category: OrderErrorCategory): Result<Order> =
        Result.failure(OrderException(category))
}
