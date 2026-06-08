package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.Cart
import com.qhathia.mobile.discovery.core.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the session [Cart] for observers (the cart dialog and the cart-count badges). */
class ObserveCartUseCase @Inject constructor(
    private val repository: CartRepository,
) {
    operator fun invoke(): Flow<Cart> = repository.observe()
}
