package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.CartShare
import com.qhathia.mobile.discovery.core.domain.repository.CartShareRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShareCartUseCaseTest {

    private class RecordingRepository(private val result: Result<CartShare>) : CartShareRepository {
        var call: Triple<String, String, List<CartItem>>? = null
        override suspend fun shareCart(
            name: String,
            phone: String,
            items: List<CartItem>,
        ): Result<CartShare> {
            call = Triple(name, phone, items)
            return result
        }
    }

    private val items = listOf(CartItem("p1", "Taladro", 100.0, "t.jpg", "AVAILABLE", 1, 99))

    @Test
    fun given_args_when_invoked_then_delegates_to_repository() = runTest {
        val share = CartShare("SENT", "https://r2.dev/c.pdf", "wamid.1")
        val repository = RecordingRepository(Result.success(share))

        val result = ShareCartUseCase(repository)("Ana", "987654321", items)

        assertEquals(share, result.getOrNull())
        assertEquals(Triple("Ana", "987654321", items), repository.call)
    }
}
