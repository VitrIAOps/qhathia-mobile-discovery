package com.qhathia.mobile.discovery.core.data.order

import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.OrderErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.OrderException
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SttException
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.OrderItemResponseDto
import com.qhathia.mobile.discovery.core.network.dto.OrderResponseDto
import com.qhathia.mobile.discovery.core.network.service.OrderApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.UnknownHostException

class OrderRepositoryImplTest {

    private val sessionTokenProvider = mockk<SessionTokenProvider>(relaxUnitFun = true)
    private val orderApiService = mockk<OrderApiService>()

    private fun repository() = OrderRepositoryImpl(sessionTokenProvider, orderApiService)

    private val items = listOf(
        CartItem("p1", "Taladro", 100.0, "t.jpg", "AVAILABLE", 2, 99),
    )

    private fun response() = OrderResponseDto(
        id = "o1",
        status = "PENDING",
        subtotal = 200.0,
        total = 200.0,
        customerName = "Ana",
        customerPhone = "987654321",
        items = listOf(
            OrderItemResponseDto(
                id = "i1",
                productId = "p1",
                productName = "Taladro",
                quantity = 2,
                unitPriceSnapshot = 100.0,
                lineTotal = 200.0,
            ),
        ),
    )

    @Test
    fun given_valid_order_when_created_then_maps_to_domain() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { orderApiService.createOrder("jwt", any()) } returns NetworkResult.Success(response())

        val order = repository().createOrder("Ana", "987654321", items).getOrNull()!!

        assertEquals("o1", order.id)
        assertEquals("PENDING", order.status)
        assertEquals(1, order.items.size)
    }

    @Test
    fun given_422_when_created_then_order_api_category() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { orderApiService.createOrder(any(), any()) } returns NetworkResult.HttpError(422, "Validation")

        val result = repository().createOrder("Ana", "987654321", items)

        assertEquals(OrderErrorCategory.OrderApi, (result.exceptionOrNull() as OrderException).category)
    }

    @Test
    fun given_400_when_created_then_unavailable_category() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { orderApiService.createOrder(any(), any()) } returns NetworkResult.HttpError(400, "Bad Request")

        val result = repository().createOrder("Ana", "987654321", items)

        assertEquals(OrderErrorCategory.Unavailable, (result.exceptionOrNull() as OrderException).category)
    }

    @Test
    fun given_409_product_not_sellable_when_created_then_unavailable_category() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { orderApiService.createOrder(any(), any()) } returns NetworkResult.HttpError(409, "Conflict")

        val result = repository().createOrder("Ana", "987654321", items)

        assertEquals(OrderErrorCategory.Unavailable, (result.exceptionOrNull() as OrderException).category)
    }

    @Test
    fun given_404_when_created_then_not_found_category() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { orderApiService.createOrder(any(), any()) } returns NetworkResult.HttpError(404, "Not Found")

        val result = repository().createOrder("Ana", "987654321", items)

        assertEquals(OrderErrorCategory.NotFound, (result.exceptionOrNull() as OrderException).category)
    }

    @Test
    fun given_500_when_created_then_order_api_category() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { orderApiService.createOrder(any(), any()) } returns NetworkResult.HttpError(500, "Server Error")

        val result = repository().createOrder("Ana", "987654321", items)

        assertEquals(OrderErrorCategory.OrderApi, (result.exceptionOrNull() as OrderException).category)
    }

    @Test
    fun given_connectivity_failure_when_created_then_network_category() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { orderApiService.createOrder(any(), any()) } returns
            NetworkResult.NetworkError(UnknownHostException("no dns"), NetworkErrorKind.Connectivity)

        val result = repository().createOrder("Ana", "987654321", items)

        assertEquals(OrderErrorCategory.Network, (result.exceptionOrNull() as OrderException).category)
    }

    @Test
    fun given_timeout_when_created_then_order_api_category() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { orderApiService.createOrder(any(), any()) } returns
            NetworkResult.NetworkError(RuntimeException("slow"), NetworkErrorKind.Timeout)

        val result = repository().createOrder("Ana", "987654321", items)

        assertEquals(OrderErrorCategory.OrderApi, (result.exceptionOrNull() as OrderException).category)
    }

    @Test
    fun given_network_token_failure_when_created_then_network_category() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns
            Result.failure(SttException(SttErrorCategory.Network))

        val result = repository().createOrder("Ana", "987654321", items)

        assertEquals(OrderErrorCategory.Network, (result.exceptionOrNull() as OrderException).category)
    }

    @Test
    fun given_401_when_created_then_fails_without_retry() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { orderApiService.createOrder("jwt", any()) } returns NetworkResult.HttpError(401, "Unauthorized")

        val result = repository().createOrder("Ana", "987654321", items)

        assertEquals(OrderErrorCategory.OrderApi, (result.exceptionOrNull() as OrderException).category)
        coVerify(exactly = 1) { orderApiService.createOrder(any(), any()) }
    }
}
