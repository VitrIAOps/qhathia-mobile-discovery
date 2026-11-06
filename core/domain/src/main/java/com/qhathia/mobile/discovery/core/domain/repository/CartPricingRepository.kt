package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.ProductSnapshot

/** Port for the store's current products, used to refresh and recheck the basket in one request. */
interface CartPricingRepository {

    /**
     * Every active product of the store, keyed by id. A product missing from the map was deleted or
     * deactivated. Fails on a network or server error.
     */
    suspend fun snapshots(): Result<Map<String, ProductSnapshot>>
}
