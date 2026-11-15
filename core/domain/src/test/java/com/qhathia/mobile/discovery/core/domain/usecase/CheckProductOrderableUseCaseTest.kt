package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.DetailErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.DetailException
import com.qhathia.mobile.discovery.core.testing.FakeProductDetailRepository
import com.qhathia.mobile.discovery.core.testing.ProductDetailFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CheckProductOrderableUseCaseTest {

    private val repository = FakeProductDetailRepository()
    private val check = CheckProductOrderableUseCase(repository)

    @Test
    fun product_found_is_current() = runTest {
        val detail = ProductDetailFixtures.productDetail(id = "p1", availability = "LOW_STOCK", stock = 3)
        repository.result = Result.success(detail)

        assertEquals(ProductCheck.Current(detail), check("p1"))
        assertEquals(listOf("p1"), repository.requestedIds)
    }

    @Test
    fun product_not_found_is_gone() = runTest {
        repository.result = Result.failure(DetailException(DetailErrorCategory.NotFound))

        assertEquals(ProductCheck.Gone, check("p1"))
    }

    @Test
    fun any_other_failure_is_unknown() = runTest {
        repository.result = Result.failure(DetailException(DetailErrorCategory.Network))
        assertNull(check("p1"))

        repository.result = Result.failure(DetailException(DetailErrorCategory.DetailApi))
        assertNull(check("p1"))

        repository.result = Result.failure(IllegalStateException("boom"))
        assertNull(check("p1"))
    }
}
