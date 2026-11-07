package com.qhathia.mobile.discovery.core.data.cart

import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.ProductSnapshot
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.CatalogProductDto
import com.qhathia.mobile.discovery.core.network.service.ProductApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class CartPricingRepositoryImplTest {

    private val tokens = mockk<SessionTokenProvider>()
    private val api = mockk<ProductApiService>()
    private val repository = CartPricingRepositoryImpl(tokens, api)

    @Test
    fun maps_the_price_the_customer_pays_and_the_stock() = runTest {
        coEvery { tokens.accessToken() } returns Result.success("jwt")
        coEvery { api.listProducts("jwt") } returns NetworkResult.Success(
            listOf(
                CatalogProductDto(id = "p1", price = 100.0, discountedPrice = 80.0, availability = "AVAILABLE", stock = 3),
                CatalogProductDto(id = "p2", price = 50.0, availability = "CONSULT"),
            ),
        )

        val snapshots = repository.snapshots().getOrThrow()

        assertEquals(ProductSnapshot("p1", 80.0, "AVAILABLE", 3), snapshots["p1"])
        assertEquals(ProductSnapshot("p2", 50.0, "CONSULT", null), snapshots["p2"])
        coVerify(exactly = 1) { api.listProducts(any()) }
    }

    @Test
    fun network_failure_fails() = runTest {
        coEvery { tokens.accessToken() } returns Result.success("jwt")
        coEvery { api.listProducts(any()) } returns NetworkResult.NetworkError(IOException("offline"), NetworkErrorKind.Connectivity)

        assertTrue(repository.snapshots().isFailure)
    }

    @Test
    fun server_error_fails() = runTest {
        coEvery { tokens.accessToken() } returns Result.success("jwt")
        coEvery { api.listProducts(any()) } returns NetworkResult.HttpError(500, "boom")

        assertTrue(repository.snapshots().isFailure)
    }
}
