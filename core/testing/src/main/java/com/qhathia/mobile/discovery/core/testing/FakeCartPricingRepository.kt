package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.ProductSnapshot
import com.qhathia.mobile.discovery.core.domain.repository.CartPricingRepository
import kotlinx.coroutines.CompletableDeferred

/**
 * In-memory [CartPricingRepository]. Answers [result] (the store's products keyed by id) and counts the
 * calls in [calls]. Set [gate] to hold the answer until the test completes it.
 */
class FakeCartPricingRepository(
    var result: Result<Map<String, ProductSnapshot>> = Result.success(emptyMap()),
) : CartPricingRepository {

    var calls: Int = 0
        private set

    /** When set, [snapshots] suspends until it completes. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun snapshots(): Result<Map<String, ProductSnapshot>> {
        calls++
        gate?.await()
        return result
    }

    /** Test helper: answers these snapshots. */
    fun answer(vararg snapshots: ProductSnapshot) {
        result = Result.success(snapshots.associateBy { it.id })
    }
}
