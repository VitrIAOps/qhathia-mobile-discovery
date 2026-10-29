package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.CartShareErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.ShareOutcome
import com.qhathia.mobile.discovery.core.domain.model.ShareRejection
import com.qhathia.mobile.discovery.core.domain.repository.CartShareRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShareQuoteUseCaseTest {

    /** Answers with [answers] in order, repeating the last one; records every call. */
    private class ScriptedRepository(private vararg val answers: ShareOutcome) : CartShareRepository {
        val calls = mutableListOf<Triple<String, String, List<CartItem>>>()
        override suspend fun shareCart(name: String, phone: String, items: List<CartItem>): ShareOutcome {
            calls += Triple(name, phone, items)
            return answers[minOf(calls.size, answers.size) - 1]
        }
    }

    private val items = listOf(CartItem("p1", "Taladro", 100.0, "t.jpg", "AVAILABLE", 1, 99))
    private val delivered = ShareOutcome.Delivered("https://r2.dev/c.pdf")
    private val timeout = ShareOutcome.Failed(CartShareErrorCategory.ShareApi)

    private suspend fun share(repository: CartShareRepository) =
        ShareQuoteUseCase(repository)("Ana", "987654321", items)

    @Test
    fun given_sent_when_invoked_then_delivered_with_one_request() = runTest {
        val repository = ScriptedRepository(delivered)

        assertEquals(delivered, share(repository))
        assertEquals(listOf(Triple("Ana", "987654321", items)), repository.calls)
    }

    @Test
    fun given_400_when_invoked_then_one_request() = runTest {
        val repository = ScriptedRepository(ShareOutcome.Rejected(ShareRejection.Unavailable))

        assertEquals(ShareOutcome.Rejected(ShareRejection.Unavailable), share(repository))
        assertEquals(1, repository.calls.size)
    }

    @Test
    fun given_422_when_invoked_then_one_request() = runTest {
        val repository = ScriptedRepository(ShareOutcome.Rejected(ShareRejection.Invalid))

        share(repository)

        assertEquals(1, repository.calls.size)
    }

    @Test
    fun given_failed_status_when_invoked_then_not_delivered_with_one_request() = runTest {
        val notDelivered = ShareOutcome.NotDelivered("https://r2.dev/c.pdf", "FAILED")
        val repository = ScriptedRepository(notDelivered)

        assertEquals(notDelivered, share(repository))
        assertEquals(1, repository.calls.size)
    }

    @Test
    fun given_two_timeouts_then_sent_when_invoked_then_three_requests_and_delivered() = runTest {
        val repository = ScriptedRepository(timeout, timeout, delivered)

        assertEquals(delivered, share(repository))
        assertEquals(3, repository.calls.size)
    }

    @Test
    fun given_always_failing_when_invoked_then_stops_after_max_attempts() = runTest {
        val repository = ScriptedRepository(ShareOutcome.Failed(CartShareErrorCategory.Network))

        assertEquals(ShareOutcome.Failed(CartShareErrorCategory.Network), share(repository))
        assertEquals(ShareQuoteUseCase.MAX_ATTEMPTS, repository.calls.size)
    }

    @Test
    fun given_timeout_then_rejected_when_invoked_then_stops_at_rejection() = runTest {
        val repository = ScriptedRepository(timeout, ShareOutcome.Rejected(ShareRejection.NotFound))

        assertEquals(ShareOutcome.Rejected(ShareRejection.NotFound), share(repository))
        assertEquals(2, repository.calls.size)
    }
}
