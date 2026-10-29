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
 * Obtains the DEVICE session JWT via the shared [SessionTokenProvider]. On a `401` the cached token is
 * dropped and the call is sent once more with a fresh token. Retries for transient failures are not
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

        var response = cartShareApiService.shareCart(token, request)
        if (response is NetworkResult.HttpError && response.code == HTTP_UNAUTHORIZED) {
            sessionTokenProvider.clear()
            val refreshed = sessionTokenProvider.accessToken(forceRefresh = true)
                .getOrElse { return CartShareErrorMapper.fromTokenFailure(it) }
            response = cartShareApiService.shareCart(refreshed, request)
        }

        return when (response) {
            is NetworkResult.Success -> response.data.toOutcome()
            is NetworkResult.HttpError -> CartShareErrorMapper.fromHttpError(response.code)
            is NetworkResult.NetworkError -> CartShareErrorMapper.fromNetworkError(response.kind)
        }
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
