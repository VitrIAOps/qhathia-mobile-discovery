package com.qhathia.mobile.discovery.feature.cart

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.ShareOutcome
import com.qhathia.mobile.discovery.core.domain.model.ShareRejection
import com.qhathia.mobile.discovery.core.domain.usecase.ClearCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.CreateOrderUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RemoveCartItemUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ShareQuoteUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.UpdateCartItemQuantityUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateConsumerNameUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateConsumerPhoneUseCase
import com.qhathia.mobile.discovery.core.testing.CartFixtures
import com.qhathia.mobile.discovery.core.testing.FakeCartRepository
import com.qhathia.mobile.discovery.core.testing.FakeCartShareRepository
import com.qhathia.mobile.discovery.core.testing.FakeOrderRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEffect
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEvent
import com.qhathia.mobile.discovery.feature.cart.model.OrderSubmission
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

    private fun viewModel(
        repository: FakeCartRepository = FakeCartRepository(),
        orderRepository: FakeOrderRepository = FakeOrderRepository(),
        shareRepository: FakeCartShareRepository = FakeCartShareRepository(),
    ) = CartViewModel(
        observeCart = ObserveCartUseCase(repository),
        updateQuantity = UpdateCartItemQuantityUseCase(repository),
        removeItem = RemoveCartItemUseCase(repository),
        validateName = ValidateConsumerNameUseCase(),
        validatePhone = ValidateConsumerPhoneUseCase(),
        createOrder = CreateOrderUseCase(orderRepository),
        shareQuote = ShareQuoteUseCase(shareRepository),
        clearCart = ClearCartUseCase(repository),
    )

    /** Enters valid consumer data so the order can be placed. */
    private fun CartViewModel.enterValidData() {
        onEvent(CartUiEvent.NameChanged("Ana Pérez"))
        onEvent(CartUiEvent.PhoneChanged("987 654 321"))
    }

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
    fun given_valid_data_when_share_then_success_with_summary() = runTest(mainDispatcher.dispatcher) {
        val shareRepository = FakeCartShareRepository()
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), shareRepository = shareRepository)
        vm.enterValidData()

        vm.onEvent(CartUiEvent.ShareWhatsApp)

        val submission = vm.uiState.value.submission
        assertTrue(submission is OrderSubmission.Success)
        assertEquals(2, (submission as OrderSubmission.Success).summary.size)
        // The share success uses the quote-sent copy, never the order copy (CAR-008).
        assertTrue(submission.isQuoteOnly)
        assertEquals(1, shareRepository.callCount)
        // The ViewModel forwards the raw form value; the data layer normalizes it before the API.
        assertEquals("987 654 321", shareRepository.lastCall?.phone)
    }

    @Test
    fun given_failure_when_share_then_error() = runTest(mainDispatcher.dispatcher) {
        val shareRepository = FakeCartShareRepository().apply { failWith() }
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), shareRepository = shareRepository)
        vm.enterValidData()

        vm.onEvent(CartUiEvent.ShareWhatsApp)

        assertEquals(OrderSubmission.Error, vm.uiState.value.submission)
        assertEquals(3, shareRepository.callCount)
    }

    @Test
    fun given_share_answers_failed_when_share_then_not_delivered_and_no_retry() =
        runTest(mainDispatcher.dispatcher) {
            val shareRepository = FakeCartShareRepository().apply { notDelivered("FAILED") }
            val vm = viewModel(FakeCartRepository(CartFixtures.cart()), shareRepository = shareRepository)
            vm.enterValidData()

            vm.onEvent(CartUiEvent.ShareWhatsApp)

            assertEquals(OrderSubmission.QuoteNotDelivered, vm.uiState.value.submission)
            assertEquals(1, shareRepository.callCount)
        }

    @Test
    fun given_share_answers_stub_when_share_then_not_delivered() = runTest(mainDispatcher.dispatcher) {
        val shareRepository = FakeCartShareRepository().apply { notDelivered("STUB") }
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), shareRepository = shareRepository)
        vm.enterValidData()

        vm.onEvent(CartUiEvent.ShareWhatsApp)

        assertEquals(OrderSubmission.QuoteNotDelivered, vm.uiState.value.submission)
    }

    @Test
    fun given_share_rejected_when_share_then_error_sent_once() = runTest(mainDispatcher.dispatcher) {
        val shareRepository = FakeCartShareRepository(ShareOutcome.Rejected(ShareRejection.Invalid))
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), shareRepository = shareRepository)
        vm.enterValidData()

        vm.onEvent(CartUiEvent.ShareWhatsApp)

        assertEquals(OrderSubmission.Error, vm.uiState.value.submission)
        assertEquals(1, shareRepository.callCount)
    }

    @Test
    fun given_quote_not_delivered_when_dismiss_then_idle_and_cart_kept() = runTest(mainDispatcher.dispatcher) {
        val shareRepository = FakeCartShareRepository().apply { notDelivered() }
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), shareRepository = shareRepository)
        vm.enterValidData()
        vm.onEvent(CartUiEvent.ShareWhatsApp)

        vm.onEvent(CartUiEvent.DismissError)

        assertEquals(OrderSubmission.Idle, vm.uiState.value.submission)
        assertEquals(2, vm.uiState.value.items.size)
    }

    @Test
    fun given_invalid_data_when_share_then_no_submission() = runTest(mainDispatcher.dispatcher) {
        val shareRepository = FakeCartShareRepository()
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), shareRepository = shareRepository)
        // No consumer data entered → sharing disabled.

        vm.onEvent(CartUiEvent.ShareWhatsApp)

        assertEquals(OrderSubmission.Idle, vm.uiState.value.submission)
        assertEquals(0, shareRepository.callCount)
    }

    @Test
    fun given_resolved_submission_when_share_again_then_ignored() = runTest(mainDispatcher.dispatcher) {
        val orderRepository = FakeOrderRepository()
        val shareRepository = FakeCartShareRepository()
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), orderRepository, shareRepository)
        vm.enterValidData()

        vm.onEvent(CartUiEvent.PlaceOrder)      // → Success (order + its own share); submission no longer Idle
        vm.onEvent(CartUiEvent.ShareWhatsApp)   // blocked: a submission already resolved this session

        assertEquals(1, orderRepository.callCount)
        assertEquals(1, shareRepository.callCount) // only the order flow's own share ran
    }

    @Test
    fun given_valid_data_when_place_order_then_also_shares_and_marks_delivered() = runTest(mainDispatcher.dispatcher) {
        val orderRepository = FakeOrderRepository()
        val shareRepository = FakeCartShareRepository()
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), orderRepository, shareRepository)
        vm.enterValidData()

        vm.onEvent(CartUiEvent.PlaceOrder)

        val submission = vm.uiState.value.submission
        assertTrue(submission is OrderSubmission.Success)
        assertTrue((submission as OrderSubmission.Success).shareDelivered)
        assertEquals(1, orderRepository.callCount)
        assertEquals(1, shareRepository.callCount)
    }

    @Test
    fun given_order_ok_but_share_fails_when_place_order_then_success_not_delivered_after_retries() =
        runTest(mainDispatcher.dispatcher) {
            val orderRepository = FakeOrderRepository()
            val shareRepository = FakeCartShareRepository().apply { failWith() }
            val vm = viewModel(FakeCartRepository(CartFixtures.cart()), orderRepository, shareRepository)
            vm.enterValidData()

            vm.onEvent(CartUiEvent.PlaceOrder)

            val submission = vm.uiState.value.submission
            assertTrue(submission is OrderSubmission.Success) // the order was created…
            assertFalse((submission as OrderSubmission.Success).shareDelivered) // …but the quote never landed
            assertEquals(1, orderRepository.callCount)
            assertEquals(3, shareRepository.callCount) // retried up to the max before giving up
        }

    @Test
    fun given_order_ok_but_share_answers_failed_when_place_order_then_success_with_fallback_copy() =
        runTest(mainDispatcher.dispatcher) {
            val orderRepository = FakeOrderRepository()
            val shareRepository = FakeCartShareRepository().apply { notDelivered("FAILED") }
            val vm = viewModel(FakeCartRepository(CartFixtures.cart()), orderRepository, shareRepository)
            vm.enterValidData()

            vm.onEvent(CartUiEvent.PlaceOrder)

            val submission = vm.uiState.value.submission as OrderSubmission.Success
            assertFalse(submission.isQuoteOnly) // order copy
            assertFalse(submission.shareDelivered) // seller fallback, not the check-your-phone prompt
            assertEquals(1, shareRepository.callCount) // FAILED is final, not retried
        }

    @Test
    fun given_success_when_accept_then_submission_resets_to_idle() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(CartFixtures.cart())
        val vm = viewModel(repository)
        vm.enterValidData()
        vm.onEvent(CartUiEvent.ShareWhatsApp)
        assertTrue(vm.uiState.value.submission is OrderSubmission.Success)

        vm.onEvent(CartUiEvent.AcceptSuccess)

        // Phase reset so a reopened cart is not stuck on the success screen.
        assertEquals(OrderSubmission.Idle, vm.uiState.value.submission)
    }

    @Test
    fun given_valid_data_when_place_order_then_success_with_summary() = runTest(mainDispatcher.dispatcher) {
        val orderRepository = FakeOrderRepository()
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), orderRepository)
        vm.enterValidData()

        vm.onEvent(CartUiEvent.PlaceOrder)

        val submission = vm.uiState.value.submission
        assertTrue(submission is OrderSubmission.Success)
        assertEquals(2, (submission as OrderSubmission.Success).summary.size)
        assertEquals(1, orderRepository.callCount)
        // The ViewModel forwards the raw form value; the data layer normalizes it before the API.
        assertEquals("987 654 321", orderRepository.lastCall?.phone)
    }

    @Test
    fun given_failure_when_place_order_then_error() = runTest(mainDispatcher.dispatcher) {
        val orderRepository = FakeOrderRepository().apply { failWith() }
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), orderRepository)
        vm.enterValidData()

        vm.onEvent(CartUiEvent.PlaceOrder)

        assertEquals(OrderSubmission.Error, vm.uiState.value.submission)
    }

    @Test
    fun given_invalid_data_when_place_order_then_no_submission() = runTest(mainDispatcher.dispatcher) {
        val orderRepository = FakeOrderRepository()
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), orderRepository)
        // No consumer data entered → order disabled.

        vm.onEvent(CartUiEvent.PlaceOrder)

        assertEquals(OrderSubmission.Idle, vm.uiState.value.submission)
        assertEquals(0, orderRepository.callCount)
    }

    @Test
    fun given_non_idle_when_place_order_again_then_ignored() = runTest(mainDispatcher.dispatcher) {
        val orderRepository = FakeOrderRepository()
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), orderRepository)
        vm.enterValidData()

        vm.onEvent(CartUiEvent.PlaceOrder) // → Success
        vm.onEvent(CartUiEvent.PlaceOrder) // blocked: submission is not Idle

        assertEquals(1, orderRepository.callCount)
    }

    @Test
    fun given_success_when_accept_then_clears_cart_and_returns_home() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(CartFixtures.cart())
        val vm = viewModel(repository)
        vm.enterValidData()
        vm.onEvent(CartUiEvent.PlaceOrder)

        vm.uiEffect.test {
            vm.onEvent(CartUiEvent.AcceptSuccess)
            assertEquals(CartUiEffect.ReturnHome, awaitItem())
        }
        assertTrue(repository.current.isEmpty)
    }

    @Test
    fun given_success_when_accept_then_consumer_form_is_cleared() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(CartFixtures.cart())
        val vm = viewModel(repository)
        vm.enterValidData()
        vm.onEvent(CartUiEvent.PlaceOrder)

        vm.onEvent(CartUiEvent.AcceptSuccess)

        // The consumer data is wiped so the next session starts from a blank, disabled form.
        val state = vm.uiState.value
        assertEquals("", state.name)
        assertEquals("", state.phone)
        assertFalse(state.isOrderEnabled)
    }

    @Test
    fun given_error_when_cancel_then_clears_cart_and_returns_home() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(CartFixtures.cart())
        val vm = viewModel(repository, FakeOrderRepository().apply { failWith() })
        vm.enterValidData()
        vm.onEvent(CartUiEvent.PlaceOrder)

        vm.uiEffect.test {
            vm.onEvent(CartUiEvent.CancelOrder)
            assertEquals(CartUiEffect.ReturnHome, awaitItem())
        }
        assertTrue(repository.current.isEmpty)
    }

    @Test
    fun given_error_when_dismiss_then_idle_and_cart_preserved() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(CartFixtures.cart())
        val vm = viewModel(repository, FakeOrderRepository().apply { failWith() })
        vm.enterValidData()
        vm.onEvent(CartUiEvent.PlaceOrder)
        assertEquals(OrderSubmission.Error, vm.uiState.value.submission)

        vm.onEvent(CartUiEvent.DismissError)

        assertEquals(OrderSubmission.Idle, vm.uiState.value.submission)
        assertFalse(repository.current.isEmpty)
    }
}
