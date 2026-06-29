package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.repository.ProductDetailRepository
import javax.inject.Inject

/**
 * Fetches up to 5 complementary products for a product by id, delegating to the
 * [ProductDetailRepository]. Used by the product-detail "Podrías acompañar este producto con…" strip.
 */
class GetProductSuggestionsUseCase @Inject constructor(
    private val repository: ProductDetailRepository,
) {
    suspend operator fun invoke(id: String): Result<List<Product>> = repository.getSuggestions(id)
}
