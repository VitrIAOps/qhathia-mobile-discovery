package com.qhathia.mobile.discovery.core.data.detail

import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.DetailErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.DetailException
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.MediaDto
import com.qhathia.mobile.discovery.core.network.dto.ProductDetailResponseDto
import com.qhathia.mobile.discovery.core.network.dto.ProductSuggestionsResponseDto
import com.qhathia.mobile.discovery.core.network.dto.SuggestedProductDto
import com.qhathia.mobile.discovery.core.network.service.ProductApiService
import io.mockk.coEvery
import io.mockk.coVerify
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
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
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
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { productApiService.getProduct(any(), any()) } returns NetworkResult.HttpError(404, "Not Found")

        val result = repository().getProduct("missing")

        assertEquals(DetailErrorCategory.NotFound, (result.exceptionOrNull() as DetailException).category)
    }

    @Test
    fun given_server_error_when_get_then_detail_api_category() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { productApiService.getProduct(any(), any()) } returns NetworkResult.HttpError(500, "Server Error")

        val result = repository().getProduct("p1")

        assertEquals(DetailErrorCategory.DetailApi, (result.exceptionOrNull() as DetailException).category)
    }

    @Test
    fun given_connectivity_failure_when_get_then_network_category() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { productApiService.getProduct(any(), any()) } returns
            NetworkResult.NetworkError(UnknownHostException("no dns"), NetworkErrorKind.Connectivity)

        val result = repository().getProduct("p1")

        assertEquals(DetailErrorCategory.Network, (result.exceptionOrNull() as DetailException).category)
    }

    @Test
    fun given_401_when_get_then_fails_without_retry() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { productApiService.getProduct("jwt", "p1") } returns NetworkResult.HttpError(401, "Unauthorized")

        val result = repository().getProduct("p1")

        assertEquals(DetailErrorCategory.DetailApi, (result.exceptionOrNull() as DetailException).category)
        coVerify(exactly = 1) { productApiService.getProduct(any(), any()) }
    }

    private fun suggested(id: String) = SuggestedProductDto(
        id = id,
        categoryName = "Accesorios",
        name = "Producto $id",
        price = 19.9,
        availability = "AVAILABLE",
        primaryImageUrl = "$id.jpg",
    )

    @Test
    fun given_suggestions_when_get_then_maps_and_caps_at_five() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { productApiService.getSuggestions("jwt", "p1") } returns NetworkResult.Success(
            ProductSuggestionsResponseDto(
                productId = "p1",
                suggestions = (1..7).map { suggested("s$it") },
                source = "llm",
            ),
        )

        val products = repository().getSuggestions("p1").getOrNull()!!

        assertEquals(5, products.size)
        assertEquals("s1", products.first().id)
        assertEquals("Producto s1", products.first().name)
    }

    @Test
    fun given_empty_suggestions_when_get_then_empty_success() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { productApiService.getSuggestions(any(), any()) } returns NetworkResult.Success(
            ProductSuggestionsResponseDto(productId = "p1", suggestions = emptyList(), source = "empty"),
        )

        val result = repository().getSuggestions("p1")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun given_server_error_when_get_suggestions_then_detail_api_failure() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { productApiService.getSuggestions(any(), any()) } returns NetworkResult.HttpError(500, "Server Error")

        val result = repository().getSuggestions("p1")

        assertEquals(DetailErrorCategory.DetailApi, (result.exceptionOrNull() as DetailException).category)
    }

    @Test
    fun given_401_when_get_suggestions_then_fails_without_retry() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { productApiService.getSuggestions("jwt", "p1") } returns NetworkResult.HttpError(401, "Unauthorized")

        val result = repository().getSuggestions("p1")

        assertEquals(DetailErrorCategory.DetailApi, (result.exceptionOrNull() as DetailException).category)
        coVerify(exactly = 1) { productApiService.getSuggestions(any(), any()) }
    }
}
