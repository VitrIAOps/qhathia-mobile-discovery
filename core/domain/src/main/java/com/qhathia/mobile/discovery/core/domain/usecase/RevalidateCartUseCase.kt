package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.DetailErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.DetailException
import com.qhathia.mobile.discovery.core.domain.repository.ProductDetailRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

/**
 * Rechecks the basket after the server rejected an order or share for an unavailable product (`400` /
 * `404`), so the cart can flag the culprits instead of looping on a generic error (CAR-023/025).
 *
 * Calls `GET /products/{id}` for each line, at most [MAX_CONCURRENT_REQUESTS] at a time. A line is
 * unavailable when its product is inactive, not `AVAILABLE`, or not found. A line whose detail could
 * not be read (network, server error) is not flagged: the recheck only reports what it knows.
 *
 * @return the product ids of the unavailable lines; empty when nothing could be blamed.
 */
class RevalidateCartUseCase @Inject constructor(
    private val repository: ProductDetailRepository,
) {
    suspend operator fun invoke(items: List<CartItem>): Set<String> = coroutineScope {
        val permits = Semaphore(MAX_CONCURRENT_REQUESTS)
        items.map { item ->
            async { permits.withPermit { item.productId.takeIf { isUnavailable(it) } } }
        }.awaitAll().filterNotNull().toSet()
    }

    private suspend fun isUnavailable(productId: String): Boolean =
        repository.getProduct(productId).fold(
            onSuccess = { !IsOrderableUseCase.isOrderable(it.availability, it.active) },
            onFailure = { (it as? DetailException)?.category == DetailErrorCategory.NotFound },
        )

    private companion object {
        /** Keeps the recheck light on the server; the basket has at most 20 lines. */
        const val MAX_CONCURRENT_REQUESTS = 4
    }
}
