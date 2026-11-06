package com.qhathia.mobile.discovery.core.data.cart

import com.qhathia.mobile.discovery.core.domain.model.Cart
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.CartPriceChange
import com.qhathia.mobile.discovery.core.domain.model.CartRefresh
import com.qhathia.mobile.discovery.core.domain.model.ProductSnapshot
import com.qhathia.mobile.discovery.core.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * [CartRepository] backed by the in-memory [CartSessionLocalDataSource]. The mutations are synchronous
 * StateFlow updates; the `suspend` modifiers keep the port flexible for a future persistent backing.
 */
class CartRepositoryImpl @Inject constructor(
    private val localDataSource: CartSessionLocalDataSource,
) : CartRepository {

    override fun observe(): Flow<Cart> = localDataSource.observe()

    override suspend fun addProduct(item: CartItem) = localDataSource.addProduct(item)

    override suspend fun setQuantity(productId: String, quantity: Int) =
        localDataSource.setQuantity(productId, quantity)

    override suspend fun removeProduct(productId: String) = localDataSource.removeProduct(productId)

    override suspend fun clear() = localDataSource.clear()

    override suspend fun refresh(snapshots: Map<String, ProductSnapshot>): CartRefresh =
        localDataSource.refresh(snapshots)

    override suspend fun updatePrices(changes: List<CartPriceChange>) = localDataSource.updatePrices(changes)
}
