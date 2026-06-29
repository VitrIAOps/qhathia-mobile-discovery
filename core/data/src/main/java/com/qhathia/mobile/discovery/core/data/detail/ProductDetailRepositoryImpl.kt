package com.qhathia.mobile.discovery.core.data.detail

import android.util.Log
import com.qhathia.mobile.discovery.core.data.BuildConfig
import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.DetailErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.DetailException
import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductDetail
import com.qhathia.mobile.discovery.core.domain.repository.ProductDetailRepository
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.ProductApiService
import javax.inject.Inject

/**
 * Calls the Qhathia product API (`GET /api/v1/products/{id}`) and maps the result to a [ProductDetail].
 *
 * Obtains the session JWT via the shared [SessionTokenProvider] (the interim auth seam reused from the
 * STT/search pipeline). On a `401` the cached token is dropped and the call is retried once with a
 * fresh login. Failures surface as a failed [Result] carrying a [DetailException] tagged with the
 * matching [DetailErrorCategory] so the presentation layer can pick the recovery UX (`404` →
 * [DetailErrorCategory.NotFound]).
 */
class ProductDetailRepositoryImpl @Inject constructor(
    private val sessionTokenProvider: SessionTokenProvider,
    private val productApiService: ProductApiService,
) : ProductDetailRepository {

    override suspend fun getProduct(id: String): Result<ProductDetail> {
        val token = sessionTokenProvider.accessToken()
            .getOrElse { return failure(DetailErrorMapper.fromTokenFailure(it)) }

        var response = productApiService.getProduct(token, id)
        if (response is NetworkResult.HttpError && response.code == HTTP_UNAUTHORIZED) {
            sessionTokenProvider.clear()
            val refreshed = sessionTokenProvider.accessToken(forceRefresh = true)
                .getOrElse { return failure(DetailErrorMapper.fromTokenFailure(it)) }
            response = productApiService.getProduct(refreshed, id)
        }

        return when (response) {
            is NetworkResult.Success -> success(response.data.toDomain())
            is NetworkResult.HttpError ->
                if (response.code == HTTP_NOT_FOUND) failure(DetailErrorCategory.NotFound)
                else failure(DetailErrorCategory.DetailApi)
            is NetworkResult.NetworkError -> failure(DetailErrorMapper.fromNetworkError(response.kind))
        }
    }

    override suspend fun getSuggestions(id: String): Result<List<Product>> {
        val token = sessionTokenProvider.accessToken()
            .getOrElse { return Result.failure(DetailException(DetailErrorMapper.fromTokenFailure(it))) }

        var response = productApiService.getSuggestions(token, id)
        if (response is NetworkResult.HttpError && response.code == HTTP_UNAUTHORIZED) {
            sessionTokenProvider.clear()
            val refreshed = sessionTokenProvider.accessToken(forceRefresh = true)
                .getOrElse { return Result.failure(DetailException(DetailErrorMapper.fromTokenFailure(it))) }
            response = productApiService.getSuggestions(refreshed, id)
        }

        return when (response) {
            is NetworkResult.Success -> {
                val products = response.data.suggestions.map { it.toDomain() }.take(MAX_SUGGESTIONS)
                if (BuildConfig.DEBUG) Log.d(TAG, "suggestions result: id=$id count=${products.size}")
                Result.success(products)
            }
            is NetworkResult.HttpError -> Result.failure(DetailException(DetailErrorCategory.DetailApi))
            is NetworkResult.NetworkError ->
                Result.failure(DetailException(DetailErrorMapper.fromNetworkError(response.kind)))
        }
    }

    private fun success(detail: ProductDetail): Result<ProductDetail> {
        if (BuildConfig.DEBUG) Log.d(TAG, "detail result: id=${detail.id} images=${detail.images.size}")
        return Result.success(detail)
    }

    private fun failure(category: DetailErrorCategory): Result<ProductDetail> {
        if (BuildConfig.DEBUG) Log.d(TAG, "detail result: failure=$category")
        return Result.failure(DetailException(category))
    }

    private companion object {
        const val TAG = "ProductDetail"
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_NOT_FOUND = 404
        const val MAX_SUGGESTIONS = 5
    }
}
