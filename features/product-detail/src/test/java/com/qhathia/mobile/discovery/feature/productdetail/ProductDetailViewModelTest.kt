package com.qhathia.mobile.discovery.feature.productdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.DetailErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.DetailException
import com.qhathia.mobile.discovery.core.domain.usecase.GetProductDetailUseCase
import com.qhathia.mobile.discovery.core.testing.FakeProductDetailRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.core.testing.ProductDetailFixtures
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiEffect
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiEvent
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiState
import com.qhathia.mobile.discovery.feature.productdetail.navigation.ARG_PRODUCT_ID
import com.qhathia.mobile.discovery.feature.productdetail.ui.ProductDetailViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private fun viewModel(
        productId: String = "p1",
        repository: FakeProductDetailRepository = FakeProductDetailRepository(),
    ) = ProductDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ARG_PRODUCT_ID to productId)),
        getProductDetail = GetProductDetailUseCase(repository),
    )

    @Test
    fun given_product_when_initialized_then_loaded_with_related() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductDetailRepository(
            result = Result.success(ProductDetailFixtures.productDetail(id = "p1", name = "Taladro")),
        )
        val vm = viewModel(repository = repository)

        val status = vm.uiState.value.status
        assertTrue(status is ProductDetailUiState.Status.Loaded)
        assertEquals("Taladro", (status as ProductDetailUiState.Status.Loaded).detail.name)
        assertTrue(status.relatedProducts.isNotEmpty()) // mock related products
        assertEquals(listOf("p1"), repository.requestedIds)
    }

    @Test
    fun given_404_when_initialized_then_not_found_status() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductDetailRepository(
            result = Result.failure(DetailException(DetailErrorCategory.NotFound)),
        )
        val vm = viewModel(repository = repository)

        val status = vm.uiState.value.status
        assertEquals(ProductDetailUiState.ErrorKind.NotFound, (status as ProductDetailUiState.Status.Error).kind)
    }

    @Test
    fun given_network_error_when_initialized_then_network_status() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductDetailRepository(
            result = Result.failure(DetailException(DetailErrorCategory.Network)),
        )
        val vm = viewModel(repository = repository)

        val status = vm.uiState.value.status
        assertEquals(ProductDetailUiState.ErrorKind.Network, (status as ProductDetailUiState.Status.Error).kind)
    }

    @Test
    fun given_error_when_retry_succeeds_then_loaded() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductDetailRepository(
            result = Result.failure(DetailException(DetailErrorCategory.DetailApi)),
        )
        val vm = viewModel(repository = repository)
        assertTrue(vm.uiState.value.status is ProductDetailUiState.Status.Error)

        repository.result = Result.success(ProductDetailFixtures.productDetail())
        vm.onEvent(ProductDetailUiEvent.Retry)

        assertTrue(vm.uiState.value.status is ProductDetailUiState.Status.Loaded)
        assertEquals(2, repository.callCount)
    }

    @Test
    fun given_back_when_tapped_then_emits_navigate_back() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(ProductDetailUiEvent.Back)
            assertEquals(ProductDetailUiEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun given_confirm_new_search_then_emits_navigate_home_idle() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(ProductDetailUiEvent.NewSearch)
            assertTrue(vm.uiState.value.showCloseConfirmDialog)
            vm.onEvent(ProductDetailUiEvent.ConfirmCloseSearch)
            assertEquals(ProductDetailUiEffect.NavigateHomeIdle, awaitItem())
        }
        assertFalse(vm.uiState.value.showCloseConfirmDialog)
    }

    @Test
    fun given_dialog_when_dismissed_then_hidden() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.onEvent(ProductDetailUiEvent.NewSearch)
        vm.onEvent(ProductDetailUiEvent.DismissDialog)

        assertFalse(vm.uiState.value.showCloseConfirmDialog)
    }
}
