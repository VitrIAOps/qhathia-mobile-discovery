package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.CartShare
import com.qhathia.mobile.discovery.core.domain.model.CartShareErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.CartShareException
import com.qhathia.mobile.discovery.core.domain.repository.CartShareRepository

/**
 * Scriptable [CartShareRepository] for use-case / ViewModel tests. Records the last call and returns a
 * pre-programmed [result]. Defaults to success with a `SENT` share carrying a fake PDF URL.
 */
class FakeCartShareRepository(
    var result: Result<CartShare>? = null,
) : CartShareRepository {

    /** The arguments of the last [shareCart] call, or `null` if never called. */
    var lastCall: Call? = null
        private set

    /** Number of times [shareCart] was invoked. */
    var callCount: Int = 0
        private set

    data class Call(val name: String, val phone: String, val items: List<CartItem>)

    override suspend fun shareCart(
        name: String,
        phone: String,
        items: List<CartItem>,
    ): Result<CartShare> {
        callCount++
        lastCall = Call(name, phone, items)
        return result ?: Result.success(
            CartShare(status = "SENT", pdfUrl = "https://r2.dev/cotizacion.pdf", messageId = "wamid.1"),
        )
    }

    /** Programs the next call to fail with [category]. */
    fun failWith(category: CartShareErrorCategory = CartShareErrorCategory.ShareApi) {
        result = Result.failure(CartShareException(category))
    }
}
