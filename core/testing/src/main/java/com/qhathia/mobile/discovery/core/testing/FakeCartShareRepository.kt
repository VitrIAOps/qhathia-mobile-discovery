package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.CartShareErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.ShareOutcome
import com.qhathia.mobile.discovery.core.domain.repository.CartShareRepository

/**
 * Scriptable [CartShareRepository] for use-case / ViewModel tests. Records each call. Answers with the
 * queued outcomes in order ([enqueue]), then with [outcome] for every later call. Defaults to a
 * delivered (`SENT`) share.
 */
class FakeCartShareRepository(
    var outcome: ShareOutcome = ShareOutcome.Delivered(PDF_URL),
) : CartShareRepository {

    private val queue = ArrayDeque<ShareOutcome>()

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
    ): ShareOutcome {
        callCount++
        lastCall = Call(name, phone, items)
        return queue.removeFirstOrNull() ?: outcome
    }

    /** Queues one-off answers for the next calls, in order, before falling back to [outcome]. */
    fun enqueue(vararg outcomes: ShareOutcome) {
        queue.addAll(outcomes)
    }

    /** Answers every call with a transient failure of [category]. */
    fun failWith(category: CartShareErrorCategory = CartShareErrorCategory.ShareApi) {
        outcome = ShareOutcome.Failed(category)
    }

    /** Answers every call with a `201` that WhatsApp did not receive. */
    fun notDelivered(status: String = "FAILED") {
        outcome = ShareOutcome.NotDelivered(PDF_URL, status)
    }

    companion object {
        const val PDF_URL = "https://r2.dev/cotizacion.pdf"
    }
}
