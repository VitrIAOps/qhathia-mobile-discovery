package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.ProductSearchConstants
import com.qhathia.mobile.discovery.core.domain.model.SearchErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.SearchException
import com.qhathia.mobile.discovery.core.domain.model.StructuredSearchFilters
import com.qhathia.mobile.discovery.core.testing.FakeProductSearchRepository
import com.qhathia.mobile.discovery.core.testing.ProductFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SearchProductsUseCaseTest {

    private val repository = FakeProductSearchRepository()
    private val useCase = SearchProductsUseCase(repository)

    @Test
    fun given_query_when_invoke_then_returns_repository_result() = runTest {
        val expected = ProductFixtures.result()
        repository.result = Result.success(expected)

        val result = useCase("taladro bosch")

        assertEquals(expected, result.getOrNull())
        assertEquals("taladro bosch", repository.queries.single())
    }

    @Test
    fun given_query_over_cap_when_invoke_then_truncated_to_max_chars() = runTest {
        val longQuery = "a".repeat(ProductSearchConstants.MAX_QUERY_CHARS + 100)

        useCase(longQuery)

        assertEquals(ProductSearchConstants.MAX_QUERY_CHARS, repository.queries.single().length)
    }

    @Test
    fun given_padded_query_when_invoke_then_trimmed() = runTest {
        useCase("   taladro   ")

        assertEquals("taladro", repository.queries.single())
    }

    @Test
    fun given_repository_failure_when_invoke_then_propagates() = runTest {
        repository.result = Result.failure(SearchException(SearchErrorCategory.SearchApi))

        val result = useCase("taladro")

        assertTrue(result.isFailure)
        assertEquals(
            SearchErrorCategory.SearchApi,
            (result.exceptionOrNull() as SearchException).category,
        )
    }

    @Test
    fun filters_are_passed_through_and_empty_ones_dropped() = runTest {
        val useCase = SearchProductsUseCase(repository)

        useCase("taladro", filters = StructuredSearchFilters(brands = listOf("Bosch")))
        useCase("taladro", filters = StructuredSearchFilters())

        assertEquals(listOf(StructuredSearchFilters(brands = listOf("Bosch")), null), repository.filters)
    }
}
