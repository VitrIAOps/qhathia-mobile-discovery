package com.qhathia.mobile.discovery.core.data.search

import android.util.Log
import com.qhathia.mobile.discovery.core.data.BuildConfig
import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.ProductSearchResult
import com.qhathia.mobile.discovery.core.domain.model.StructuredSearchFilters
import com.qhathia.mobile.discovery.core.domain.model.SearchErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SearchException
import com.qhathia.mobile.discovery.core.domain.repository.CustomerSessionRepository
import com.qhathia.mobile.discovery.core.domain.repository.ProductSearchRepository
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.ConversationSearchApiService
import javax.inject.Inject

/**
 * Calls the Qhathia conversational search API and maps the result to a [ProductSearchResult].
 *
 * Obtains the DEVICE session JWT via the shared [SessionTokenProvider]. A `401` is not retried: an unlinked tablet ends its session through the HTTP client's unlink signal.
 * Failures surface as a failed [Result] carrying a [SearchException] tagged with the matching
 * [SearchErrorCategory] so the presentation layer can pick the recovery UX.
 */
class ProductSearchRepositoryImpl @Inject constructor(
    private val sessionTokenProvider: SessionTokenProvider,
    private val searchApiService: ConversationSearchApiService,
    private val customerSession: CustomerSessionRepository,
) : ProductSearchRepository {

    override suspend fun search(
        query: String,
        language: String,
        filters: StructuredSearchFilters?,
    ): Result<ProductSearchResult> {
        logQuery(query)

        val token = sessionTokenProvider.accessToken()
            .getOrElse { return failure(SearchErrorMapper.fromTokenFailure(it)) }

        val response = searchApiService.search(
            token,
            query,
            language,
            sessionId = customerSession.currentSessionId(),
            filters = filters?.toDto(),
        )

        return when (response) {
            is NetworkResult.Success -> success(response.data.toDomain())
            is NetworkResult.HttpError -> failure(SearchErrorCategory.SearchApi)
            is NetworkResult.NetworkError -> failure(SearchErrorMapper.fromNetworkError(response.kind))
        }
    }

    private fun success(result: ProductSearchResult): Result<ProductSearchResult> {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "search result: count=${result.products.size} noResults=${result.noResults}")
        }
        return Result.success(result)
    }

    private fun failure(category: SearchErrorCategory): Result<ProductSearchResult> {
        if (BuildConfig.DEBUG) Log.d(TAG, "search result: failure=$category")
        return Result.failure(SearchException(category))
    }

    private fun logQuery(query: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "search query: length=${query.length}")
    }

    private companion object {
        const val TAG = "ProductSearch"
    }
}
