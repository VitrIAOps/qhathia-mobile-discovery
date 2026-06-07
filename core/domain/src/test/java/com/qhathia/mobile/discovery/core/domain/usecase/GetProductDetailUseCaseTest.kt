package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.DetailErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.DetailException
import com.qhathia.mobile.discovery.core.testing.FakeProductDetailRepository
import com.qhathia.mobile.discovery.core.testing.ProductDetailFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetProductDetailUseCaseTest {

    @Test
    fun given_product_when_invoked_then_returns_detail_for_id() = runTest {
        val repository = FakeProductDetailRepository(
            result = Result.success(ProductDetailFixtures.productDetail(id = "p42", name = "Martillo")),
        )
        val useCase = GetProductDetailUseCase(repository)

        val result = useCase("p42")

        assertEquals("Martillo", result.getOrNull()!!.name)
        assertEquals(listOf("p42"), repository.requestedIds)
    }

    @Test
    fun given_failure_when_invoked_then_propagates_failure() = runTest {
        val repository = FakeProductDetailRepository(
            result = Result.failure(DetailException(DetailErrorCategory.NotFound)),
        )
        val useCase = GetProductDetailUseCase(repository)

        val result = useCase("missing")

        assertTrue(result.isFailure)
        assertEquals(DetailErrorCategory.NotFound, (result.exceptionOrNull() as DetailException).category)
    }
}
