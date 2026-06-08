package com.qhathia.mobile.discovery.feature.cart

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RemoveCartItemUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.UpdateCartItemQuantityUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateConsumerNameUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateConsumerPhoneUseCase
import com.qhathia.mobile.discovery.core.testing.CartFixtures
import com.qhathia.mobile.discovery.core.testing.FakeCartRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEffect
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEvent
import com.qhathia.mobile.discovery.feature.cart.ui.CartViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private fun viewModel(repository: FakeCartRepository = FakeCartRepository()) = CartViewModel(
        observeCart = ObserveCartUseCase(repository),
        updateQuantity = UpdateCartItemQuantityUseCase(repository),
        removeItem = RemoveCartItemUseCase(repository),
        validateName = ValidateConsumerNameUseCase(),
        validatePhone = ValidateConsumerPhoneUseCase(),
    )

    @Test
    fun given_cart_when_observed_then_state_has_items_and_totals() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(CartFixtures.cart())
        val vm = viewModel(repository)

        val state = vm.uiState.value
        assertEquals(2, state.items.size)
        assertEquals(3, state.totalItemCount) // 1 + 2
        assertEquals(189.0 + 49.9 * 2, state.totalAmount)
    }

    @Test
    fun given_line_when_increased_then_quantity_grows() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(CartFixtures.cart(listOf(CartFixtures.cartItem(productId = "p1", quantity = 1))))
        val vm = viewModel(repository)

        vm.onEvent(CartUiEvent.IncreaseLine("p1"))

        assertEquals(2, vm.uiState.value.items.first().quantity)
    }

    @Test
    fun given_line_at_one_when_decreased_then_line_removed() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(CartFixtures.cart(listOf(CartFixtures.cartItem(productId = "p1", quantity = 1))))
        val vm = viewModel(repository)

        vm.onEvent(CartUiEvent.DecreaseLine("p1"))

        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun given_line_at_max_when_increased_then_quantity_capped() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(
            CartFixtures.cart(listOf(CartFixtures.cartItem(productId = "p1", quantity = 5, maxQuantity = 5))),
        )
        val vm = viewModel(repository)

        vm.onEvent(CartUiEvent.IncreaseLine("p1"))

        assertEquals(5, vm.uiState.value.items.first().quantity)
    }

    @Test
    fun given_invalid_then_valid_data_when_entered_then_order_enables() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        assertFalse(vm.uiState.value.isOrderEnabled)

        vm.onEvent(CartUiEvent.NameChanged("Ana"))
        vm.onEvent(CartUiEvent.PhoneChanged("123"))
        assertFalse(vm.uiState.value.isOrderEnabled) // phone invalid

        vm.onEvent(CartUiEvent.PhoneChanged("987 654 321"))
        assertTrue(vm.uiState.value.isOrderEnabled)
    }

    @Test
    fun given_line_tap_when_open_detail_then_emits_navigate() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()))

        vm.uiEffect.test {
            vm.onEvent(CartUiEvent.OpenLineDetail("p1"))
            assertEquals(CartUiEffect.NavigateToDetail("p1"), awaitItem())
        }
    }

    @Test
    fun given_keep_shopping_then_emits_dismiss() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()

        vm.uiEffect.test {
            vm.onEvent(CartUiEvent.KeepShopping)
            assertEquals(CartUiEffect.Dismiss, awaitItem())
        }
    }

    @Test
    fun given_place_order_and_share_then_no_op_no_effect() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(CartFixtures.cart())
        val vm = viewModel(repository)

        vm.uiEffect.test {
            vm.onEvent(CartUiEvent.PlaceOrder)
            vm.onEvent(CartUiEvent.ShareWhatsApp)
            expectNoEvents()
        }
        // The basket is untouched by the render-only actions.
        assertFalse(repository.current.isEmpty)
    }
}
