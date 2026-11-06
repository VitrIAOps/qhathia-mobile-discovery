package com.qhathia.mobile.discovery.core.data.share

import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.CartShareErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.ShareOutcome
import com.qhathia.mobile.discovery.core.domain.model.ShareRejection
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.ShareCartResponseDto
import com.qhathia.mobile.discovery.core.network.service.CartShareApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.UnknownHostException

/**
 * Classification of each share answer into a [ShareOutcome]. The wire side (JSON body, HTTP status and
 * timeout to [NetworkResult]) is covered by `CartShareApiServiceTest` over a Ktor MockEngine.
 */
class CartShareRepositoryImplTest {

    private val sessionTokenProvider = mockk<SessionTokenProvider>(relaxUnitFun = true).apply {
        coEvery { accessToken() } returns Result.success("jwt")
    }
    private val cartShareApiService = mockk<CartShareApiService>()

    private fun repository() = CartShareRepositoryImpl(sessionTokenProvider, cartShareApiService)

    private val items = listOf(CartItem("p1", "Taladro", 100.0, "t.jpg", "AVAILABLE", 2, 99))

    private fun response(status: String) = ShareCartResponseDto(
        status = status,
        messageId = null,
        pdfUrl = "https://r2.dev/cotizacion.pdf",
    )

    private suspend fun shareWith(answer: NetworkResult<ShareCartResponseDto>): ShareOutcome {
        coEvery { cartShareApiService.shareCart(any(), any()) } returns answer
        return repository().shareCart("Ana", "987654321", items)
    }

    @Test
    fun given_sent_when_shared_then_delivered() = runTest {
        assertEquals(
            ShareOutcome.Delivered("https://r2.dev/cotizacion.pdf"),
            shareWith(NetworkResult.Success(response("SENT"))),
        )
    }

    @Test
    fun given_201_failed_when_shared_then_not_delivered() = runTest {
        assertEquals(
            ShareOutcome.NotDelivered("https://r2.dev/cotizacion.pdf", "FAILED"),
            shareWith(NetworkResult.Success(response("FAILED"))),
        )
    }

    @Test
    fun given_201_stub_when_shared_then_not_delivered() = runTest {
        assertEquals(
            ShareOutcome.NotDelivered("https://r2.dev/cotizacion.pdf", "STUB"),
            shareWith(NetworkResult.Success(response("STUB"))),
        )
    }

    @Test
    fun given_unknown_status_when_shared_then_not_delivered() = runTest {
        assertEquals(
            ShareOutcome.NotDelivered("https://r2.dev/cotizacion.pdf", "QUEUED"),
            shareWith(NetworkResult.Success(response("QUEUED"))),
        )
    }

    @Test
    fun given_400_when_shared_then_rejected_unavailable_sent_once() = runTest {
        assertEquals(ShareOutcome.Rejected(ShareRejection.Unavailable), shareWith(NetworkResult.HttpError(400, "Bad Request")))
        coVerify(exactly = 1) { cartShareApiService.shareCart(any(), any()) }
    }

    @Test
    fun given_404_when_shared_then_rejected_not_found() = runTest {
        assertEquals(ShareOutcome.Rejected(ShareRejection.NotFound), shareWith(NetworkResult.HttpError(404, "Not Found")))
    }

    @Test
    fun given_422_when_shared_then_rejected_invalid_sent_once() = runTest {
        assertEquals(ShareOutcome.Rejected(ShareRejection.Invalid), shareWith(NetworkResult.HttpError(422, "Unprocessable")))
        coVerify(exactly = 1) { cartShareApiService.shareCart(any(), any()) }
    }

    @Test
    fun given_409_product_not_sellable_when_shared_then_rejected_unavailable_sent_once() = runTest {
        assertEquals(ShareOutcome.Rejected(ShareRejection.Unavailable), shareWith(NetworkResult.HttpError(409, "Conflict")))
        coVerify(exactly = 1) { cartShareApiService.shareCart(any(), any()) }
    }

    @Test
    fun given_other_4xx_when_shared_then_rejected_other() = runTest {
        assertEquals(ShareOutcome.Rejected(ShareRejection.Other), shareWith(NetworkResult.HttpError(403, "Forbidden")))
    }

    @Test
    fun given_500_when_shared_then_failed_share_api() = runTest {
        assertEquals(
            ShareOutcome.Failed(CartShareErrorCategory.ShareApi),
            shareWith(NetworkResult.HttpError(500, "Internal Server Error")),
        )
    }

    @Test
    fun given_timeout_when_shared_then_failed_share_api() = runTest {
        assertEquals(
            ShareOutcome.Failed(CartShareErrorCategory.ShareApi),
            shareWith(NetworkResult.NetworkError(RuntimeException("slow"), NetworkErrorKind.Timeout)),
        )
    }

    @Test
    fun given_connectivity_loss_when_shared_then_failed_network() = runTest {
        assertEquals(
            ShareOutcome.Failed(CartShareErrorCategory.Network),
            shareWith(NetworkResult.NetworkError(UnknownHostException("no dns"), NetworkErrorKind.Connectivity)),
        )
    }

    @Test
    fun given_network_token_failure_when_shared_then_failed_network_without_request() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.failure(SttException(SttErrorCategory.Network))

        assertEquals(ShareOutcome.Failed(CartShareErrorCategory.Network), shareWith(NetworkResult.Success(response("SENT"))))
        coVerify(exactly = 0) { cartShareApiService.shareCart(any(), any()) }
    }

    @Test
    fun given_401_when_shared_then_rejected_without_retry() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { cartShareApiService.shareCart("jwt", any()) } returns NetworkResult.HttpError(401, "Unauthorized")

        val outcome = repository().shareCart("Ana", "987654321", items)

        assertEquals(ShareOutcome.Rejected(ShareRejection.Other), outcome)
        coVerify(exactly = 1) { cartShareApiService.shareCart(any(), any()) }
    }
}
