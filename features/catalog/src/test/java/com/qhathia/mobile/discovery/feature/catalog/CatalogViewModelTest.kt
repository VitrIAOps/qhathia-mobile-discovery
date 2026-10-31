package com.qhathia.mobile.discovery.feature.catalog

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.CatalogConstants
import com.qhathia.mobile.discovery.core.domain.model.CatalogErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.CatalogException
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.domain.usecase.AddToCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.EndCustomerSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GetProductCatalogUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCatalogRefreshUseCase
import com.qhathia.mobile.discovery.core.testing.CatalogFixtures
import com.qhathia.mobile.discovery.core.testing.FakeCartRepository
import com.qhathia.mobile.discovery.core.testing.FakeCatalogRefreshSignal
import com.qhathia.mobile.discovery.core.testing.FakeCustomerSessionRepository
import com.qhathia.mobile.discovery.core.testing.FakeProductCatalogRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.core.testing.ProductFixtures
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
        refreshSignal: FakeCatalogRefreshSignal = FakeCatalogRefreshSignal(),
        sessionRepository: FakeCustomerSessionRepository = FakeCustomerSessionRepository(),
    ) = CatalogViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ARG_SOURCE to source.name)),
        getProductCatalog = GetProductCatalogUseCase(repository),
        observeCart = ObserveCartUseCase(cartRepository),
        addToCart = AddToCartUseCase(cartRepository),
        observeCatalogRefresh = ObserveCatalogRefreshUseCase(refreshSignal),
        endCustomerSession = EndCustomerSessionUseCase(cartRepository, sessionRepository),
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

    // --- Remote "Actualizar catálogo" (device-remote-commands) ---

    @Test
    fun given_remote_refresh_then_reloads_keeping_selected_tab() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductCatalogRepository(Result.success(CatalogFixtures.catalog()))
        val signal = FakeCatalogRefreshSignal()
        val vm = viewModel(repository = repository, refreshSignal = signal)
        vm.onEvent(CatalogUiEvent.TabSelected(CatalogTab.categoryId("c2")))
        val fresh = listOf(
            CatalogFixtures.category(id = "c2", name = "Eléctrico", products = listOf(ProductFixtures.product(id = "e9"))),
        )
        repository.result = Result.success(fresh)

        signal.request()

        val state = vm.uiState.value
        assertEquals(2, repository.callCount)
        assertEquals(CatalogTab.categoryId("c2"), state.selectedTabId)
        assertEquals(listOf("e9"), state.visibleProducts.map { it.id })
        assertFalse(state.isLoading)
    }

    @Test
    fun given_remote_refresh_without_selected_tab_then_falls_back_to_todo() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductCatalogRepository(Result.success(CatalogFixtures.catalog()))
        val signal = FakeCatalogRefreshSignal()
        val vm = viewModel(repository = repository, refreshSignal = signal)
        vm.onEvent(CatalogUiEvent.TabSelected(CatalogTab.categoryId("c2")))
        repository.result = Result.success(listOf(CatalogFixtures.category(id = "c1")))

        signal.request()

        assertEquals(CatalogTab.ALL_ID, vm.uiState.value.selectedTabId)
    }

    @Test
    fun given_remote_refresh_fails_then_keeps_products_and_no_error() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductCatalogRepository(Result.success(CatalogFixtures.catalog()))
        val signal = FakeCatalogRefreshSignal()
        val vm = viewModel(repository = repository, refreshSignal = signal)
        val before = vm.uiState.value.visibleProducts
        repository.result = Result.failure(CatalogException(CatalogErrorCategory.Network))

        signal.request()

        val state = vm.uiState.value
        assertFalse(state.isNetworkError)
        assertEquals(before, state.visibleProducts)
    }

    @Test
    fun given_error_screen_then_remote_refresh_retries_load() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductCatalogRepository(Result.failure(CatalogException(CatalogErrorCategory.Network)))
        val signal = FakeCatalogRefreshSignal()
        val vm = viewModel(repository = repository, refreshSignal = signal)
        assertTrue(vm.uiState.value.isNetworkError)
        repository.result = Result.success(CatalogFixtures.catalog())

        signal.request()

        val state = vm.uiState.value
        assertFalse(state.isNetworkError)
        assertTrue(state.showContent)
    }

    @Test
    fun given_items_in_cart_when_close_confirmed_then_session_ends_before_going_home() =
        runTest(mainDispatcher.dispatcher) {
            val cartRepository = FakeCartRepository()
            val sessionRepository = FakeCustomerSessionRepository()
            val vm = viewModel(cartRepository = cartRepository, sessionRepository = sessionRepository)
            vm.onEvent(CatalogUiEvent.CartTapped("l1"))
            assertEquals(1, vm.uiState.value.cartItemCount)

            vm.uiEffect.test {
                vm.onEvent(CatalogUiEvent.NewSearch)
                vm.onEvent(CatalogUiEvent.ConfirmCloseSearch)
                assertEquals(CatalogUiEffect.NavigateHomeIdle, awaitItem())
                // Already cleared when the navigation effect arrives (CAR-040).
                assertTrue(cartRepository.current.items.isEmpty())
                assertEquals(1, sessionRepository.endCount)
            }
        }

    @Test
    fun given_empty_cart_when_back_then_session_ends_without_dialog() = runTest(mainDispatcher.dispatcher) {
        val sessionRepository = FakeCustomerSessionRepository()
        val vm = viewModel(sessionRepository = sessionRepository)

        vm.uiEffect.test {
            vm.onEvent(CatalogUiEvent.Back)
            assertEquals(CatalogUiEffect.NavigateHomeIdle, awaitItem())
        }
        assertFalse(vm.uiState.value.showCloseConfirmDialog)
        assertEquals(1, sessionRepository.endCount)
    }

    @Test
    fun given_close_dialog_when_dismissed_then_session_kept() = runTest(mainDispatcher.dispatcher) {
        val cartRepository = FakeCartRepository()
        val sessionRepository = FakeCustomerSessionRepository()
        val vm = viewModel(cartRepository = cartRepository, sessionRepository = sessionRepository)
        vm.onEvent(CatalogUiEvent.CartTapped("l1"))

        vm.onEvent(CatalogUiEvent.Back)
        vm.onEvent(CatalogUiEvent.DismissDialog)

        assertFalse(vm.uiState.value.showCloseConfirmDialog)
        assertEquals(1, cartRepository.current.totalItemCount)
        assertEquals(0, sessionRepository.endCount)
    }

    @Test
    fun given_network_error_when_retry_succeeds_then_catalog_shown() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductCatalogRepository(Result.failure(CatalogException(CatalogErrorCategory.Network)))
        val vm = viewModel(repository = repository)
        assertTrue(vm.uiState.value.isNetworkError)

        repository.result = Result.success(CatalogFixtures.catalog())
        vm.onEvent(CatalogUiEvent.Retry)

        val state = vm.uiState.value
        assertFalse(state.isNetworkError)
        assertFalse(state.isLoading)
        assertTrue(state.visibleProducts.isNotEmpty())
        assertEquals(2, repository.callCount)
    }

    @Test
    fun given_network_error_when_retry_fails_then_idle_countdown_restarts() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeProductCatalogRepository(Result.failure(CatalogException(CatalogErrorCategory.Network)))
        val vm = viewModel(repository = repository)
        val timeoutMs = CatalogConstants.ERROR_IDLE_TIMEOUT_SECONDS * 1_000

        vm.uiEffect.test {
            advanceTimeBy(timeoutMs - 5_000)
            vm.onEvent(CatalogUiEvent.Retry)
            assertTrue(vm.uiState.value.isNetworkError)
            // The first countdown would have fired here; the retry restarted it.
            advanceTimeBy(10_000)
            expectNoEvents()
            advanceTimeBy(timeoutMs)
            assertEquals(CatalogUiEffect.NavigateHomeIdle, awaitItem())
        }
        assertEquals(2, repository.callCount)
    }
}
