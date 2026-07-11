package com.qhathia.mobile.discovery.core.data.share

import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.CartShare
import com.qhathia.mobile.discovery.core.domain.model.CartShareErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.CartShareException
import com.qhathia.mobile.discovery.core.domain.repository.CartShareRepository
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.CartShareApiService
import javax.inject.Inject

/**
 * Shares the cart as a WhatsApp quote via `POST /api/v1/carts/share-pdf` and maps the result to a
 * domain [CartShare].
 *
 * Obtains the DEVICE session JWT via the shared [SessionTokenProvider] (the interim auth seam reused
 * from the STT/detail/order pipeline). On a `401` the cached token is dropped and the call is retried
 * once with a fresh login. The endpoint is fail-soft, so any `201` (even `status = FAILED`) maps to a
 * successful [CartShare]; failures surface as a failed [Result] carrying a [CartShareException] tagged
 * with the matching [CartShareErrorCategory], which the cart collapses to one simplified message.
 */
class CartShareRepositoryImpl @Inject constructor(
    private val sessionTokenProvider: SessionTokenProvider,
    private val cartShareApiService: CartShareApiService,
) : CartShareRepository {

    override suspend fun shareCart(
        name: String,
        phone: String,
        items: List<CartItem>,
    ): Result<CartShare> {
        val request = buildShareCartRequest(name, phone, items)

        val token = sessionTokenProvider.accessToken()
            .getOrElse { return failure(CartShareErrorMapper.fromTokenFailure(it)) }

        var response = cartShareApiService.shareCart(token, request)
        if (response is NetworkResult.HttpError && response.code == HTTP_UNAUTHORIZED) {
            sessionTokenProvider.clear()
            val refreshed = sessionTokenProvider.accessToken(forceRefresh = true)
                .getOrElse { return failure(CartShareErrorMapper.fromTokenFailure(it)) }
            response = cartShareApiService.shareCart(refreshed, request)
        }

        return when (response) {
            is NetworkResult.Success -> Result.success(response.data.toDomain())
            is NetworkResult.HttpError -> failure(CartShareErrorCategory.ShareApi)
            is NetworkResult.NetworkError -> failure(CartShareErrorMapper.fromNetworkError(response.kind))
        }
    }

    private fun failure(category: CartShareErrorCategory): Result<CartShare> =
        Result.failure(CartShareException(category))

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
