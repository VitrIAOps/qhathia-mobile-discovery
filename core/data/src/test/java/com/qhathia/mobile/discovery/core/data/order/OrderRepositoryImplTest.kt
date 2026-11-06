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
import com.qhathia.mobile.discovery.core.domain.model.CartPriceChange
import com.qhathia.mobile.discovery.core.domain.model.ConsumerField
import com.qhathia.mobile.discovery.core.network.dto.CreateOrderRequestDto
import com.qhathia.mobile.discovery.core.network.dto.PriceChangeDto
import com.qhathia.mobile.discovery.core.network.dto.ProblemDetailDto
import io.mockk.slot
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
    fun given_timeout_when_created_then_timeout_category() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { orderApiService.createOrder(any(), any()) } returns
            NetworkResult.NetworkError(RuntimeException("slow"), NetworkErrorKind.Timeout)

        val result = repository().createOrder("Ana", "987654321", items)

        assertEquals(OrderErrorCategory.Timeout, (result.exceptionOrNull() as OrderException).category)
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

    // --- order-price-check: error codes and the price the customer saw ---

    private suspend fun failWith(code: Int, problem: ProblemDetailDto?): OrderException {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { orderApiService.createOrder(any(), any()) } returns NetworkResult.HttpError(code, "x", problem)
        return repository().createOrder("Ana", "987654321", items).exceptionOrNull() as OrderException
    }

    @Test
    fun price_changed_carries_the_changes() = runTest {
        val e = failWith(
            409,
            ProblemDetailDto(
                errorCode = "PRICE_CHANGED",
                changes = listOf(PriceChangeDto("p1", "Taladro", expectedUnitPrice = 100.0, currentUnitPrice = 90.0)),
            ),
        )

        assertEquals(OrderErrorCategory.PriceChanged, e.category)
        assertEquals(listOf(CartPriceChange("p1", "Taladro", 100.0, 90.0)), e.details.priceChanges)
    }

    @Test
    fun price_changed_without_changes_is_generic() = runTest {
        assertEquals(OrderErrorCategory.OrderApi, failWith(409, ProblemDetailDto(errorCode = "PRICE_CHANGED")).category)
    }

    @Test
    fun not_sellable_names_the_product() = runTest {
        val e = failWith(409, ProblemDetailDto(errorCode = "PRODUCT_NOT_SELLABLE", productId = "p1", productName = "Taladro", reason = "OUT_OF_STOCK"))

        assertEquals(OrderErrorCategory.ProductNotSellable, e.category)
        assertEquals("p1", e.details.productId)
        assertEquals("Taladro", e.details.productName)
    }

    @Test
    fun stock_insufficient_carries_the_units_left() = runTest {
        val e = failWith(422, ProblemDetailDto(errorCode = "STOCK_INSUFFICIENT", productId = "p1", productName = "Taladro", available = 3, requested = 5))

        assertEquals(OrderErrorCategory.StockInsufficient, e.category)
        assertEquals(3, e.details.available)
    }

    @Test
    fun validation_error_lists_the_fields() = runTest {
        val e = failWith(422, ProblemDetailDto(errorCode = "VALIDATION_ERROR", fieldErrors = mapOf("customerName" to "max 200", "other" to "x")))

        assertEquals(OrderErrorCategory.Validation, e.category)
        assertEquals(setOf(ConsumerField.Name), e.details.invalidFields)
    }

    @Test
    fun conflict_without_body_rechecks() = runTest {
        assertEquals(OrderErrorCategory.Unavailable, failWith(409, null).category)
        assertEquals(OrderErrorCategory.NotFound, failWith(404, ProblemDetailDto(errorCode = "RESOURCE_NOT_FOUND")).category)
    }

    @Test
    fun request_carries_the_rounded_price_the_customer_saw() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        val sent = slot<CreateOrderRequestDto>()
        coEvery { orderApiService.createOrder(any(), capture(sent)) } returns NetworkResult.Success(response())

        repository().createOrder("Ana", "987654321", listOf(CartItem("p1", "Taladro", 79.996, "", "AVAILABLE", 1, 99)))

        assertEquals(80.0, sent.captured.items.single().expectedUnitPrice)
    }
}
