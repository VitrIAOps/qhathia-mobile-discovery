package com.qhathia.mobile.discovery.core.data.share

import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.CartShareErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.CartShareException
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.ShareCartResponseDto
import com.qhathia.mobile.discovery.core.network.service.CartShareApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.UnknownHostException

class CartShareRepositoryImplTest {

    private val sessionTokenProvider = mockk<SessionTokenProvider>(relaxUnitFun = true)
    private val cartShareApiService = mockk<CartShareApiService>()

    private fun repository() = CartShareRepositoryImpl(sessionTokenProvider, cartShareApiService)

    private val items = listOf(
        CartItem("p1", "Taladro", 100.0, "t.jpg", "AVAILABLE", 2, 99),
    )

    private fun response(status: String = "SENT") = ShareCartResponseDto(
        status = status,
        messageId = "wamid.1",
        pdfUrl = "https://r2.dev/cotizacion.pdf",
    )

    @Test
    fun given_valid_share_when_sent_then_maps_to_domain() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { cartShareApiService.shareCart("jwt", any()) } returns NetworkResult.Success(response())

        val share = repository().shareCart("Ana", "987654321", items).getOrNull()!!

        assertEquals("SENT", share.status)
        assertEquals("https://r2.dev/cotizacion.pdf", share.pdfUrl)
        assertEquals("wamid.1", share.messageId)
    }

    @Test
    fun given_201_failed_status_when_sent_then_still_success_fail_soft() = runTest {
        // The endpoint is fail-soft: a 201 with status=FAILED still carries the PDF → treat as success.
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { cartShareApiService.shareCart(any(), any()) } returns
            NetworkResult.Success(response(status = "FAILED"))

        val result = repository().shareCart("Ana", "987654321", items)

        assertTrue(result.isSuccess)
        assertEquals("FAILED", result.getOrNull()!!.status)
    }

    @Test
    fun given_422_when_sent_then_share_api_category() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { cartShareApiService.shareCart(any(), any()) } returns NetworkResult.HttpError(422, "Validation")

        val result = repository().shareCart("Ana", "987654321", items)

        assertEquals(CartShareErrorCategory.ShareApi, (result.exceptionOrNull() as CartShareException).category)
    }

    @Test
    fun given_connectivity_failure_when_sent_then_network_category() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { cartShareApiService.shareCart(any(), any()) } returns
            NetworkResult.NetworkError(UnknownHostException("no dns"), NetworkErrorKind.Connectivity)

        val result = repository().shareCart("Ana", "987654321", items)

        assertEquals(CartShareErrorCategory.Network, (result.exceptionOrNull() as CartShareException).category)
    }

    @Test
    fun given_timeout_when_sent_then_share_api_category() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { cartShareApiService.shareCart(any(), any()) } returns
            NetworkResult.NetworkError(RuntimeException("slow"), NetworkErrorKind.Timeout)

        val result = repository().shareCart("Ana", "987654321", items)

        assertEquals(CartShareErrorCategory.ShareApi, (result.exceptionOrNull() as CartShareException).category)
    }

    @Test
    fun given_network_token_failure_when_sent_then_network_category() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns
            Result.failure(SttException(SttErrorCategory.Network))

        val result = repository().shareCart("Ana", "987654321", items)

        assertEquals(CartShareErrorCategory.Network, (result.exceptionOrNull() as CartShareException).category)
    }

    @Test
    fun given_401_when_sent_then_clears_token_and_retries_once() = runTest {
        coEvery { sessionTokenProvider.accessToken(false) } returns Result.success("stale")
        coEvery { sessionTokenProvider.accessToken(true) } returns Result.success("fresh")
        coEvery { cartShareApiService.shareCart("stale", any()) } returns NetworkResult.HttpError(401, "Unauthorized")
        coEvery { cartShareApiService.shareCart("fresh", any()) } returns NetworkResult.Success(response())

        val result = repository().shareCart("Ana", "987654321", items)

        assertTrue(result.isSuccess)
        assertEquals("SENT", result.getOrNull()!!.status)
    }
}
