package com.qhathia.mobile.discovery.core.data.detail

import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.DetailErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.DetailException
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.MediaDto
import com.qhathia.mobile.discovery.core.network.dto.ProductDetailResponseDto
import com.qhathia.mobile.discovery.core.network.service.ProductApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.UnknownHostException

class ProductDetailRepositoryImplTest {

    private val sessionTokenProvider = mockk<SessionTokenProvider>(relaxUnitFun = true)
    private val productApiService = mockk<ProductApiService>()

    private fun repository() = ProductDetailRepositoryImpl(sessionTokenProvider, productApiService)

    private fun response(
        id: String = "p1",
        media: List<MediaDto> = listOf(
            MediaDto(id = "m2", url = "b.jpg", position = 1, primary = false),
            MediaDto(id = "m1", url = "a.jpg", position = 0, primary = true),
        ),
    ) = ProductDetailResponseDto(
        id = id,
        name = "Taladro",
        description = "Profesional",
        price = 380.0,
        availability = "AVAILABLE",
        categoryName = "Taladros",
        media = media,
    )

    @Test
    fun given_product_when_get_then_maps_and_orders_images() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { productApiService.getProduct("jwt", "p1") } returns NetworkResult.Success(response())

        val detail = repository().getProduct("p1").getOrNull()!!

        assertEquals("Taladro", detail.name)
        assertEquals(2, detail.images.size)
        // Primary image is ordered first regardless of its position field.
        assertTrue(detail.images.first().isPrimary)
        assertEquals("a.jpg", detail.images.first().url)
    }

    @Test
    fun given_404_when_get_then_not_found_category() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { productApiService.getProduct(any(), any()) } returns NetworkResult.HttpError(404, "Not Found")

        val result = repository().getProduct("missing")

        assertEquals(DetailErrorCategory.NotFound, (result.exceptionOrNull() as DetailException).category)
    }

    @Test
    fun given_server_error_when_get_then_detail_api_category() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { productApiService.getProduct(any(), any()) } returns NetworkResult.HttpError(500, "Server Error")

        val result = repository().getProduct("p1")

        assertEquals(DetailErrorCategory.DetailApi, (result.exceptionOrNull() as DetailException).category)
    }

    @Test
    fun given_connectivity_failure_when_get_then_network_category() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { productApiService.getProduct(any(), any()) } returns
            NetworkResult.NetworkError(UnknownHostException("no dns"), NetworkErrorKind.Connectivity)

        val result = repository().getProduct("p1")

        assertEquals(DetailErrorCategory.Network, (result.exceptionOrNull() as DetailException).category)
    }

    @Test
    fun given_401_when_get_then_clears_token_and_retries_once() = runTest {
        coEvery { sessionTokenProvider.accessToken(false) } returns Result.success("stale")
        coEvery { sessionTokenProvider.accessToken(true) } returns Result.success("fresh")
        coEvery { productApiService.getProduct("stale", "p1") } returns NetworkResult.HttpError(401, "Unauthorized")
        coEvery { productApiService.getProduct("fresh", "p1") } returns NetworkResult.Success(response())

        val result = repository().getProduct("p1")

        assertTrue(result.isSuccess)
        assertEquals("Taladro", result.getOrNull()!!.name)
    }
}
