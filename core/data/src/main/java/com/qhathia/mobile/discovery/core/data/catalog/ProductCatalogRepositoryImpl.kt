package com.qhathia.mobile.discovery.core.data.catalog

import android.util.Log
import com.qhathia.mobile.discovery.core.data.BuildConfig
import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.CatalogCategory
import com.qhathia.mobile.discovery.core.domain.model.CatalogErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.CatalogException
import com.qhathia.mobile.discovery.core.domain.repository.ProductCatalogRepository
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.CatalogApiService
import javax.inject.Inject

/**
 * Calls the Qhathia catalog API and maps the result to a list of [CatalogCategory].
 *
 * Obtains the session JWT via the shared [SessionTokenProvider] (the interim auth seam reused from the
 * STT pipeline). On a `401` the cached token is dropped and the call is retried once with a fresh
 * login. Failures surface as a failed [Result] carrying a [CatalogException] tagged with the matching
 * [CatalogErrorCategory] so the presentation layer can pick the recovery UX.
 */
class ProductCatalogRepositoryImpl @Inject constructor(
    private val sessionTokenProvider: SessionTokenProvider,
    private val catalogApiService: CatalogApiService,
) : ProductCatalogRepository {

    override suspend fun getCatalog(): Result<List<CatalogCategory>> {
        val token = sessionTokenProvider.accessToken()
            .getOrElse { return failure(CatalogErrorMapper.fromTokenFailure(it)) }

        var response = catalogApiService.getCatalog(token)
        if (response is NetworkResult.HttpError && response.code == HTTP_UNAUTHORIZED) {
            sessionTokenProvider.clear()
            val refreshed = sessionTokenProvider.accessToken(forceRefresh = true)
                .getOrElse { return failure(CatalogErrorMapper.fromTokenFailure(it)) }
            response = catalogApiService.getCatalog(refreshed)
        }

        return when (response) {
            is NetworkResult.Success -> success(response.data.map { it.toDomain() })
            is NetworkResult.HttpError -> failure(CatalogErrorCategory.Generic)
            is NetworkResult.NetworkError -> failure(CatalogErrorMapper.fromNetworkError(response.kind))
        }
    }

    private fun success(categories: List<CatalogCategory>): Result<List<CatalogCategory>> {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "catalog result: categories=${categories.size} products=${categories.sumOf { it.products.size }}")
        }
        return Result.success(categories)
    }

    private fun failure(category: CatalogErrorCategory): Result<List<CatalogCategory>> {
        if (BuildConfig.DEBUG) Log.d(TAG, "catalog result: failure=$category")
        return Result.failure(CatalogException(category))
    }

    private companion object {
        const val TAG = "ProductCatalog"
        const val HTTP_UNAUTHORIZED = 401
    }
}
