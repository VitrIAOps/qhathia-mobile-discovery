package com.qhathia.mobile.discovery.core.data.share

import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.ShareOutcome
import com.qhathia.mobile.discovery.core.domain.repository.CartShareRepository
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.CartShareApiService
import javax.inject.Inject

/**
 * Shares the cart as a WhatsApp quote via `POST /api/v1/carts/share-pdf` and classifies the answer as
 * a [ShareOutcome]:
 * - `201` → [ShareOutcome.Delivered] only when `status = SENT`, otherwise [ShareOutcome.NotDelivered].
 * - `4xx` → [ShareOutcome.Rejected]; `5xx`, timeout or connectivity → [ShareOutcome.Failed].
 *
 * Obtains the DEVICE session JWT via the shared [SessionTokenProvider]. A `401` is not retried: an unlinked tablet ends its session through the HTTP client's unlink signal.
 * Retries for transient failures are not
 * done here; they belong to `ShareQuoteUseCase`.
 */
class CartShareRepositoryImpl @Inject constructor(
    private val sessionTokenProvider: SessionTokenProvider,
    private val cartShareApiService: CartShareApiService,
) : CartShareRepository {

    override suspend fun shareCart(
        name: String,
        phone: String,
        items: List<CartItem>,
    ): ShareOutcome {
        val request = buildShareCartRequest(name, phone, items)

        val token = sessionTokenProvider.accessToken()
            .getOrElse { return CartShareErrorMapper.fromTokenFailure(it) }

        val response = cartShareApiService.shareCart(token, request)

        return when (response) {
            is NetworkResult.Success -> response.data.toOutcome()
            is NetworkResult.HttpError -> CartShareErrorMapper.fromHttpError(response.code, response.problem)
            is NetworkResult.NetworkError -> CartShareErrorMapper.fromNetworkError(response.kind)
        }
    }
}
