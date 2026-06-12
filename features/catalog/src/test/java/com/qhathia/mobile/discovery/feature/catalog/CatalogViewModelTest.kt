package com.qhathia.mobile.discovery.feature.catalog

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.CatalogConstants
import com.qhathia.mobile.discovery.core.domain.model.CatalogErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.CatalogException
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.domain.usecase.AddToCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GetProductCatalogUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCartUseCase
import com.qhathia.mobile.discovery.core.testing.CatalogFixtures
import com.qhathia.mobile.discovery.core.testing.FakeCartRepository
import com.qhathia.mobile.discovery.core.testing.FakeProductCatalogRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.feature.catalog.model.CatalogTab
import com.qhathia.mobile.discovery.feature.catalog.model.CatalogUiEffect
import com.qhathia.mobile.discovery.feature.catalog.model.CatalogUiEvent
import com.qhathia.mobile.discovery.feature.catalog.navigation.ARG_SOURCE
import com.qhathia.mobile.discovery.feature.catalog.ui.CatalogViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private fun viewModel(
        source: SearchSource = SearchSource.Text,
        repository: FakeProductCatalogRepository = FakeProductCatalogRepository(),
        cartRepository: FakeCartRepository = FakeCartRepository(),
    ) = CatalogViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ARG_SOURCE to source.name)),
        getProductCatalog = GetProductCatalogUseCase(repository),
        observeCart = ObserveCartUseCase(cartRepository),
        addToCart = AddToCartUseCase(cartRepository),
    )

    @Test
    fun given_catalog_loads_then_tabs_built_and_todo_shows_all_products() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductCatalogRepository(Result.success(CatalogFixtures.catalog(discounted = 2)))
        val vm = viewModel(repository = repository)

        val state = vm.uiState.value
        // Todo + 2 categories + Descuentos.
        assertEquals(4, state.tabs.size)
        assertEquals(CatalogTab.Kind.All, state.tabs.first().kind)
        assertEquals(CatalogTab.Kind.Discounts, state.tabs.last().kind)
        assertEquals(CatalogTab.ALL_ID, state.selectedTabId)
        // Todo shows every product across categories (3 lighting + 2 electric).
        assertEquals(5, state.visibleProducts.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun given_discounts_tab_selected_then_only_discounted_products() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductCatalogRepository(Result.success(CatalogFixtures.catalog(discounted = 2)))
        val vm = viewModel(repository = repository)

        vm.onEvent(CatalogUiEvent.TabSelected(CatalogTab.DISCOUNTS_ID))

        assertEquals(2, vm.uiState.value.visibleProducts.size)
        assertFalse(vm.uiState.value.showDiscountsEmpty)
    }

    @Test
    fun given_no_discounted_products_when_discounts_tab_then_empty_message() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductCatalogRepository(Result.success(CatalogFixtures.catalog(discounted = 0)))
        val vm = viewModel(repository = repository)

        vm.onEvent(CatalogUiEvent.TabSelected(CatalogTab.DISCOUNTS_ID))

        assertTrue(vm.uiState.value.visibleProducts.isEmpty())
        assertTrue(vm.uiState.value.showDiscountsEmpty)
    }

    @Test
    fun given_category_tab_selected_then_only_that_category_products() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductCatalogRepository(Result.success(CatalogFixtures.catalog()))
        val vm = viewModel(repository = repository)

        vm.onEvent(CatalogUiEvent.TabSelected(CatalogTab.categoryId("c2")))

        assertEquals(2, vm.uiState.value.visibleProducts.size) // "Eléctrico" has 2 products
    }

    @Test
    fun given_no_categories_then_store_empty() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductCatalogRepository(Result.success(emptyList()))
        val vm = viewModel(repository = repository)

        assertTrue(vm.uiState.value.isStoreEmpty)
        assertTrue(vm.uiState.value.tabs.isEmpty())
    }

    @Test
    fun given_load_failure_then_network_error_and_idle_returns_home() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductCatalogRepository(Result.failure(CatalogException(CatalogErrorCategory.Network)))
        val vm = viewModel(repository = repository)

        assertTrue(vm.uiState.value.isNetworkError)

        vm.uiEffect.test {
            advanceTimeBy(CatalogConstants.ERROR_IDLE_TIMEOUT_SECONDS * 1_000 + 1)
            assertEquals(CatalogUiEffect.NavigateHomeIdle, awaitItem())
        }
    }

    @Test
    fun given_empty_cart_when_back_then_navigates_home_idle() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(CatalogUiEvent.Back)
            assertEquals(CatalogUiEffect.NavigateHomeIdle, awaitItem())
        }
        assertFalse(vm.uiState.value.showCloseConfirmDialog)
    }

    @Test
    fun given_items_in_cart_when_back_then_opens_close_dialog() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        vm.onEvent(CatalogUiEvent.CartTapped("l1"))

        vm.onEvent(CatalogUiEvent.Back)

        assertTrue(vm.uiState.value.showCloseConfirmDialog)
    }

    @Test
    fun given_confirm_new_search_then_navigates_home_idle() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(CatalogUiEvent.NewSearch)
            assertTrue(vm.uiState.value.showCloseConfirmDialog)
            vm.onEvent(CatalogUiEvent.ConfirmCloseSearch)
            assertEquals(CatalogUiEffect.NavigateHomeIdle, awaitItem())
        }
        assertFalse(vm.uiState.value.showCloseConfirmDialog)
    }

    @Test
    fun given_cart_tapped_then_product_added_and_count_updates() = runTest(mainDispatcher.dispatcher) {
        val cartRepository = FakeCartRepository()
        val vm = viewModel(cartRepository = cartRepository)

        vm.onEvent(CatalogUiEvent.CartTapped("l1"))

        assertEquals(listOf("l1"), cartRepository.current.items.map { it.productId })
        assertEquals(1, vm.uiState.value.cartItemCount)
    }

    @Test
    fun given_open_cart_then_emits_open_cart_effect() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(CatalogUiEvent.OpenCart)
            assertEquals(CatalogUiEffect.OpenCart, awaitItem())
        }
    }

    @Test
    fun given_query_over_cap_when_changed_then_truncated() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.onEvent(CatalogUiEvent.QueryChanged("a".repeat(CatalogConstants.MAX_QUERY_CHARS + 50)))

        assertEquals(CatalogConstants.MAX_QUERY_CHARS, vm.uiState.value.query.length)
    }

    @Test
    fun given_typed_query_when_submitted_then_hands_off_text_search() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        vm.onEvent(CatalogUiEvent.QueryChanged("taladro"))

        vm.uiEffect.test {
            vm.onEvent(CatalogUiEvent.SubmitSearch)
            assertEquals(CatalogUiEffect.NavigateToSearch("taladro", SearchSource.Text), awaitItem())
        }
    }

    @Test
    fun given_blank_query_when_submitted_then_no_handoff() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(CatalogUiEvent.SubmitSearch)
            expectNoEvents()
        }
    }

    @Test
    fun given_mic_tapped_then_hands_off_voice_search() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(CatalogUiEvent.ActivateMic)
            assertEquals(CatalogUiEffect.NavigateToSearch("", SearchSource.Voice), awaitItem())
        }
    }

    @Test
    fun given_voice_source_then_mic_animation_retained() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(source = SearchSource.Voice)

        assertTrue(vm.uiState.value.showMicAnimation)
    }

    @Test
    fun given_direct_source_then_no_mic_animation() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(source = SearchSource.Text)

        assertFalse(vm.uiState.value.showMicAnimation)
    }
}
