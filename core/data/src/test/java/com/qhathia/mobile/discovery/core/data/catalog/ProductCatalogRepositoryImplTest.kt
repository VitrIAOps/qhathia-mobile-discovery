package com.qhathia.mobile.discovery.core.data.catalog

import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.CatalogErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.CatalogException
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.CatalogCategoryDto
import com.qhathia.mobile.discovery.core.network.dto.CatalogProductDto
import com.qhathia.mobile.discovery.core.network.service.CatalogApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.UnknownHostException

class ProductCatalogRepositoryImplTest {

    private val sessionTokenProvider = mockk<SessionTokenProvider>(relaxUnitFun = true)
    private val catalogApiService = mockk<CatalogApiService>()

    private fun repository() = ProductCatalogRepositoryImpl(sessionTokenProvider, catalogApiService)

    private fun catalogDto() = listOf(
        CatalogCategoryDto(
            categoryId = "c1",
            categoryName = "Iluminación",
            products = listOf(
                CatalogProductDto(id = "p1", name = "Lámpara", price = 89.0, availability = "AVAILABLE", allowDiscountInquiry = true),
            ),
        ),
    )

    @Test
    fun given_catalog_when_load_then_maps_to_domain() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { catalogApiService.getCatalog("jwt") } returns NetworkResult.Success(catalogDto())

        val result = repository().getCatalog()

        val categories = result.getOrNull()!!
        assertEquals(1, categories.size)
        assertEquals("Iluminación", categories.first().name)
        val product = categories.first().products.first()
        assertEquals("Lámpara", product.name)
        assertTrue(product.allowDiscountInquiry)
    }

    @Test
    fun given_connectivity_failure_when_load_then_network_category() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { catalogApiService.getCatalog(any()) } returns
            NetworkResult.NetworkError(UnknownHostException("no dns"), NetworkErrorKind.Connectivity)

        val result = repository().getCatalog()

        assertEquals(CatalogErrorCategory.Network, (result.exceptionOrNull() as CatalogException).category)
    }

    @Test
    fun given_timeout_when_load_then_generic_category() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { catalogApiService.getCatalog(any()) } returns
            NetworkResult.NetworkError(RuntimeException("slow"), NetworkErrorKind.Timeout)

        val result = repository().getCatalog()

        assertEquals(CatalogErrorCategory.Generic, (result.exceptionOrNull() as CatalogException).category)
    }

    @Test
    fun given_http_error_when_load_then_generic_category() = runTest {
        coEvery { sessionTokenProvider.accessToken(any()) } returns Result.success("jwt")
        coEvery { catalogApiService.getCatalog(any()) } returns NetworkResult.HttpError(500, "Server Error")

        val result = repository().getCatalog()

        assertEquals(CatalogErrorCategory.Generic, (result.exceptionOrNull() as CatalogException).category)
    }

    @Test
    fun given_401_when_load_then_clears_token_and_retries_once() = runTest {
        coEvery { sessionTokenProvider.accessToken(false) } returns Result.success("stale")
        coEvery { sessionTokenProvider.accessToken(true) } returns Result.success("fresh")
        coEvery { catalogApiService.getCatalog("stale") } returns NetworkResult.HttpError(401, "Unauthorized")
        coEvery { catalogApiService.getCatalog("fresh") } returns NetworkResult.Success(catalogDto())

        val result = repository().getCatalog()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
    }
}
