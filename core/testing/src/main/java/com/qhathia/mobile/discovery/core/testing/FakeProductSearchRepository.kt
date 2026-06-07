package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.ProductSearchResult
import com.qhathia.mobile.discovery.core.domain.repository.ProductSearchRepository
import kotlinx.coroutines.delay

/**
 * Configurable [ProductSearchRepository] for tests.
 *
 * [result] is returned for the first (original) search. [alternativeResult], when set, is returned
 * for subsequent searches (the disguised alternative search), letting tests drive the
 * original → alternatives → unhappy flow with distinct data. [delayMillis] simulates a slow search so
 * the slow-search message and the search timeout can be exercised on virtual time.
 */
class FakeProductSearchRepository(
    var result: Result<ProductSearchResult> = Result.success(ProductFixtures.result()),
    var alternativeResult: Result<ProductSearchResult>? = null,
    var delayMillis: Long = 0,
) : ProductSearchRepository {

    val queries: MutableList<String> = mutableListOf()
    var callCount: Int = 0
        private set

    override suspend fun search(query: String, language: String): Result<ProductSearchResult> {
        queries += query
        callCount++
        if (delayMillis > 0) delay(delayMillis)
        return if (callCount > 1 && alternativeResult != null) alternativeResult!! else result
    }
}
