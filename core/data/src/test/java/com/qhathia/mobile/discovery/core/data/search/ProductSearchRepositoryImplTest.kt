package com.qhathia.mobile.discovery.core.data.search

import com.qhathia.mobile.discovery.core.data.stt.SessionTokenProvider
import com.qhathia.mobile.discovery.core.domain.model.SearchErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SearchException
import com.qhathia.mobile.discovery.core.domain.model.StructuredSearchFilters
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.ConversationSearchResponseDto
import com.qhathia.mobile.discovery.core.network.dto.SearchFiltersDto
import com.qhathia.mobile.discovery.core.network.dto.SearchProductDto
import com.qhathia.mobile.discovery.core.network.service.ConversationSearchApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.UnknownHostException

class ProductSearchRepositoryImplTest {

    private val sessionTokenProvider = mockk<SessionTokenProvider>(relaxUnitFun = true)
    private val searchApiService = mockk<ConversationSearchApiService>()

    private fun repository() = ProductSearchRepositoryImpl(sessionTokenProvider, searchApiService)

    private fun response(
        products: List<SearchProductDto> = listOf(SearchProductDto(id = "p1", name = "Taladro", price = 380.0)),
        noResults: Boolean = false,
    ) = ConversationSearchResponseDto(
        understood = true,
        results = products,
        noResults = noResults,
        displayText = "Encontré ${products.size}",
    )

    @Test
    fun given_results_when_search_then_maps_to_domain() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { searchApiService.search("jwt", "taladro", any(), any(), any()) } returns
            NetworkResult.Success(response())

        val result = repository().search("taladro")

        val domain = result.getOrNull()!!
        assertEquals(1, domain.products.size)
        assertEquals("Taladro", domain.products.first().name)
        assertEquals(380.0, domain.products.first().price)
    }

    @Test
    fun given_empty_results_when_search_then_no_results_true() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { searchApiService.search(any(), any(), any(), any(), any()) } returns
            NetworkResult.Success(response(products = emptyList(), noResults = true))

        val result = repository().search("inexistente")

        assertTrue(result.getOrNull()!!.noResults)
    }

    @Test
    fun given_connectivity_failure_when_search_then_network_category() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { searchApiService.search(any(), any(), any(), any(), any()) } returns
            NetworkResult.NetworkError(UnknownHostException("no dns"), NetworkErrorKind.Connectivity)

        val result = repository().search("taladro")

        assertEquals(SearchErrorCategory.Network, (result.exceptionOrNull() as SearchException).category)
    }

    @Test
    fun given_timeout_when_search_then_search_api_category() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { searchApiService.search(any(), any(), any(), any(), any()) } returns
            NetworkResult.NetworkError(RuntimeException("slow"), NetworkErrorKind.Timeout)

        val result = repository().search("taladro")

        assertEquals(SearchErrorCategory.SearchApi, (result.exceptionOrNull() as SearchException).category)
    }

    @Test
    fun given_http_error_when_search_then_search_api_category() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { searchApiService.search(any(), any(), any(), any(), any()) } returns
            NetworkResult.HttpError(500, "Server Error")

        val result = repository().search("taladro")

        assertEquals(SearchErrorCategory.SearchApi, (result.exceptionOrNull() as SearchException).category)
    }

    @Test
    fun given_401_when_search_then_fails_without_retry() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { searchApiService.search("jwt", any(), any(), any(), any()) } returns NetworkResult.HttpError(401, "Unauthorized")

        val result = repository().search("taladro")

        assertEquals(SearchErrorCategory.SearchApi, (result.exceptionOrNull() as SearchException).category)
        coVerify(exactly = 1) { searchApiService.search(any(), any(), any(), any(), any()) }
    }

    @Test
    fun filters_reach_the_service_as_dto() = runTest {
        coEvery { sessionTokenProvider.accessToken() } returns Result.success("jwt")
        coEvery { searchApiService.search(any(), any(), any(), any(), any()) } returns NetworkResult.Success(response())

        repository().search("taladro", "es", StructuredSearchFilters(brands = listOf("Bosch")))
        repository().search("taladro", "es", null)

        coVerify { searchApiService.search("jwt", "taladro", "es", null, SearchFiltersDto(brands = listOf("Bosch"))) }
        coVerify { searchApiService.search("jwt", "taladro", "es", null, null) }
    }
}
