package com.qhathia.mobile.discovery.feature.cart

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.Cart
import com.qhathia.mobile.discovery.core.domain.model.CartPriceChange
import com.qhathia.mobile.discovery.core.domain.model.CartQuantityCap
import com.qhathia.mobile.discovery.core.domain.model.CartShareErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.ConsumerField
import com.qhathia.mobile.discovery.core.domain.model.Order
import com.qhathia.mobile.discovery.core.domain.model.OrderErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.OrderException
import com.qhathia.mobile.discovery.core.domain.model.OrderFailureDetails
import com.qhathia.mobile.discovery.core.domain.model.OrderItem
import com.qhathia.mobile.discovery.core.domain.model.ProductSnapshot
import com.qhathia.mobile.discovery.core.domain.model.ShareOutcome
import com.qhathia.mobile.discovery.core.domain.model.ShareRejection
import com.qhathia.mobile.discovery.core.domain.usecase.CreateOrderUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.EndCustomerSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCustomerSessionEndUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RefreshCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RemoveCartItemUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RevalidateCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ShareQuoteUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.UpdateCartItemQuantityUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.UpdateCartPricesUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateConsumerNameUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateConsumerPhoneUseCase
import com.qhathia.mobile.discovery.core.testing.CartFixtures
import com.qhathia.mobile.discovery.core.testing.FakeCartPricingRepository
import com.qhathia.mobile.discovery.core.testing.FakeCartRepository
import com.qhathia.mobile.discovery.core.testing.FakeCartShareRepository
import com.qhathia.mobile.discovery.core.testing.FakeCustomerSessionRepository
import com.qhathia.mobile.discovery.core.testing.FakeOrderRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEffect
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEvent
import com.qhathia.mobile.discovery.feature.cart.model.OrderFailure
import com.qhathia.mobile.discovery.feature.cart.model.OrderSubmission
import com.qhathia.mobile.discovery.feature.cart.ui.CartViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
        sessionRepository: FakeCustomerSessionRepository = FakeCustomerSessionRepository(),
        pricing: FakeCartPricingRepository? = null,
    ): CartViewModel {
        // By default the store still sells every product in the basket, at its basket price.
        val store = pricing ?: store(repository.current)
        return CartViewModel(
            observeCart = ObserveCartUseCase(repository),
            updateQuantity = UpdateCartItemQuantityUseCase(repository),
            removeItem = RemoveCartItemUseCase(repository),
            validateName = ValidateConsumerNameUseCase(),
            validatePhone = ValidateConsumerPhoneUseCase(),
            createOrder = CreateOrderUseCase(orderRepository),
            shareQuote = ShareQuoteUseCase(shareRepository),
            endCustomerSession = EndCustomerSessionUseCase(repository, sessionRepository),
            observeSessionEnd = ObserveCustomerSessionEndUseCase(sessionRepository),
            revalidateCart = RevalidateCartUseCase(store),
            refreshCart = RefreshCartUseCase(store, repository),
            updatePrices = UpdateCartPricesUseCase(repository),
        )
    }

    /**
     * The store's products for [cart]: every line still `AVAILABLE` at its basket price, except the
     * ids in [missing] (deleted or deactivated, so absent from `GET /products`).
     */
    private fun store(cart: Cart = CartFixtures.cart(), missing: Set<String> = emptySet()) =
        FakeCartPricingRepository().apply {
            answer(
                *cart.items.filter { it.productId !in missing }
                    .map { ProductSnapshot(it.productId, it.priceUnit, "AVAILABLE", it.stock) }
                    .toTypedArray(),
            )
        }

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

        assertEquals(OrderSubmission.ShareError(), vm.uiState.value.submission)
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

        assertEquals(OrderSubmission.ShareError(), vm.uiState.value.submission)
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
    fun given_letters_typed_in_phone_then_only_digits_kept() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()))

        vm.onEvent(CartUiEvent.PhoneChanged("98a7b"))

        assertEquals("987", vm.uiState.value.phone)
    }

    @Test
    fun given_empty_basket_with_valid_data_then_actions_disabled() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(FakeCartRepository())
        vm.enterValidData()

        assertFalse(vm.uiState.value.canSubmit)
    }

    @Test
    fun given_products_and_empty_form_then_actions_disabled() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()))

        assertFalse(vm.uiState.value.canSubmit)
        vm.enterValidData()
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun given_share_error_then_entendido_returns_to_the_cart_with_data_kept() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(CartFixtures.cart())
        val vm = viewModel(repository, shareRepository = FakeCartShareRepository().apply { failWith() })
        vm.enterValidData()
        vm.onEvent(CartUiEvent.ShareWhatsApp)
        advanceUntilIdle()
        assertEquals(OrderSubmission.ShareError(), vm.uiState.value.submission)

        vm.onEvent(CartUiEvent.DismissError)

        assertEquals(OrderSubmission.Idle, vm.uiState.value.submission)
        assertEquals("Ana Pérez", vm.uiState.value.name)
        assertEquals(2, vm.uiState.value.items.size)
    }

    @Test
    fun given_order_fails_then_order_error_is_kept() = runTest(mainDispatcher.dispatcher) {
        val orderRepository = FakeOrderRepository().apply { failWith() }
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), orderRepository = orderRepository)
        vm.enterValidData()

        vm.onEvent(CartUiEvent.PlaceOrder)
        advanceUntilIdle()

        assertEquals(OrderSubmission.Error(), vm.uiState.value.submission)
    }

    @Test
    fun given_request_in_flight_when_dismissed_then_nothing_changes() = runTest(mainDispatcher.dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val shareRepository = FakeCartShareRepository().apply { awaitBeforeAnswer = gate }
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), shareRepository = shareRepository)
        vm.enterValidData()

        vm.uiEffect.test {
            vm.onEvent(CartUiEvent.ShareWhatsApp)
            runCurrent()
            assertEquals(OrderSubmission.Submitting, vm.uiState.value.submission)

            vm.onEvent(CartUiEvent.DismissRequest)
            vm.onEvent(CartUiEvent.Close)
            vm.onEvent(CartUiEvent.KeepShopping)
            runCurrent()

            assertEquals(OrderSubmission.Submitting, vm.uiState.value.submission)
            expectNoEvents()
            gate.complete(Unit)
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun given_success_when_dismissed_then_accepted_and_session_ended() = runTest(mainDispatcher.dispatcher) {
        val sessionRepository = FakeCustomerSessionRepository()
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), sessionRepository = sessionRepository)
        vm.enterValidData()
        vm.onEvent(CartUiEvent.ShareWhatsApp)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.submission is OrderSubmission.Success)

        vm.onEvent(CartUiEvent.DismissRequest)
        advanceUntilIdle()

        assertEquals(OrderSubmission.Idle, vm.uiState.value.submission)
        assertEquals(1, sessionRepository.endCount)
    }

    @Test
    fun given_error_when_dismissed_then_back_to_the_cart_summary() = runTest(mainDispatcher.dispatcher) {
        val shareRepository = FakeCartShareRepository().apply { notDelivered() }
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), shareRepository = shareRepository)
        vm.enterValidData()
        vm.onEvent(CartUiEvent.ShareWhatsApp)
        advanceUntilIdle()
        assertEquals(OrderSubmission.QuoteNotDelivered, vm.uiState.value.submission)

        vm.uiEffect.test {
            vm.onEvent(CartUiEvent.DismissRequest)
            runCurrent()

            assertEquals(OrderSubmission.Idle, vm.uiState.value.submission)
            expectNoEvents()
        }
    }

    @Test
    fun given_idle_when_dismissed_then_dialog_closes() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()))

        vm.uiEffect.test {
            vm.onEvent(CartUiEvent.DismissRequest)
            assertEquals(CartUiEffect.Dismiss, awaitItem())
        }
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
    fun given_filled_form_when_session_ends_elsewhere_then_form_and_submission_reset() =
        runTest(mainDispatcher.dispatcher) {
            // The inactivity timeout ends the session from the app shell, not from this dialog.
            val sessionRepository = FakeCustomerSessionRepository()
            val vm = viewModel(FakeCartRepository(CartFixtures.cart()), sessionRepository = sessionRepository)
            vm.enterValidData()
            vm.onEvent(CartUiEvent.ShareWhatsApp) // → Success

            sessionRepository.end()

            val state = vm.uiState.value
            assertEquals(OrderSubmission.Idle, state.submission)
            assertEquals("", state.name)
            assertEquals("", state.phone)
            assertFalse(state.isOrderEnabled)
        }

    @Test
    fun given_success_when_accept_then_ends_the_customer_session_once() = runTest(mainDispatcher.dispatcher) {
        val sessionRepository = FakeCustomerSessionRepository()
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), sessionRepository = sessionRepository)
        vm.enterValidData()
        vm.onEvent(CartUiEvent.ShareWhatsApp)

        vm.onEvent(CartUiEvent.AcceptSuccess)

        assertEquals(1, sessionRepository.endCount)
    }

    @Test
    fun given_error_when_cancel_then_ends_the_customer_session() = runTest(mainDispatcher.dispatcher) {
        val sessionRepository = FakeCustomerSessionRepository()
        val vm = viewModel(
            FakeCartRepository(CartFixtures.cart()),
            FakeOrderRepository().apply { failWith() },
            sessionRepository = sessionRepository,
        )
        vm.enterValidData()
        vm.onEvent(CartUiEvent.PlaceOrder)

        vm.onEvent(CartUiEvent.CancelOrder)

        assertEquals(1, sessionRepository.endCount)
    }

    @Test
    fun given_order_400_with_inactive_line_when_placed_then_line_flagged_and_actions_blocked() =
        runTest(mainDispatcher.dispatcher) {
            val cart = CartFixtures.cart()
            val flaggedId = cart.items.first().productId
            val orderRepository = FakeOrderRepository().apply { failWith(OrderErrorCategory.Unavailable) }
            val vm = viewModel(FakeCartRepository(cart), orderRepository, pricing = store(cart, missing = setOf(flaggedId)))
            vm.enterValidData()

            vm.onEvent(CartUiEvent.PlaceOrder)

            val state = vm.uiState.value
            assertEquals(OrderSubmission.Idle, state.submission) // back to the cart, not the error loop
            assertEquals(setOf(flaggedId), state.unavailableIds)
            assertTrue(state.hasUnavailableLines)
            assertFalse(state.canSubmit)

            vm.onEvent(CartUiEvent.PlaceOrder) // the same basket cannot be resent
            vm.onEvent(CartUiEvent.ShareWhatsApp)
            assertEquals(1, orderRepository.callCount)
        }

    @Test
    fun given_flagged_line_when_removed_then_actions_enabled_again() = runTest(mainDispatcher.dispatcher) {
        val cart = CartFixtures.cart()
        val flaggedId = cart.items.first().productId
        val vm = viewModel(
            FakeCartRepository(cart),
            FakeOrderRepository().apply { failWith(OrderErrorCategory.Unavailable) },
            pricing = store(cart, missing = setOf(flaggedId)),
        )
        vm.enterValidData()
        vm.onEvent(CartUiEvent.PlaceOrder)

        vm.onEvent(CartUiEvent.RemoveLine(flaggedId))

        val state = vm.uiState.value
        assertFalse(state.hasUnavailableLines)
        assertTrue(state.canSubmit)
        assertTrue(state.unavailableIds.isEmpty())
    }

    @Test
    fun given_order_400_with_nothing_to_blame_when_placed_then_generic_error() = runTest(mainDispatcher.dispatcher) {
        val pricing = store()
        val vm = viewModel(
            FakeCartRepository(CartFixtures.cart()),
            FakeOrderRepository().apply { failWith(OrderErrorCategory.Unavailable) },
            pricing = pricing,
        )
        vm.enterValidData()

        vm.onEvent(CartUiEvent.PlaceOrder)

        assertEquals(OrderSubmission.Error(), vm.uiState.value.submission)
        assertEquals(1, pricing.calls) // one GET /products for the whole basket
    }

    @Test
    fun given_generic_order_failure_when_placed_then_no_recheck() = runTest(mainDispatcher.dispatcher) {
        val pricing = store()
        val vm = viewModel(
            FakeCartRepository(CartFixtures.cart()),
            FakeOrderRepository().apply { failWith(OrderErrorCategory.OrderApi) },
            pricing = pricing,
        )
        vm.enterValidData()

        vm.onEvent(CartUiEvent.PlaceOrder)

        assertEquals(OrderSubmission.Error(), vm.uiState.value.submission)
        assertEquals(0, pricing.calls)
    }

    @Test
    fun given_share_404_with_missing_line_when_shared_then_line_flagged() = runTest(mainDispatcher.dispatcher) {
        val cart = CartFixtures.cart()
        val flaggedId = cart.items.last().productId
        val vm = viewModel(
            FakeCartRepository(cart),
            shareRepository = FakeCartShareRepository(ShareOutcome.Rejected(ShareRejection.NotFound)),
            pricing = store(cart, missing = setOf(flaggedId)),
        )
        vm.enterValidData()

        vm.onEvent(CartUiEvent.ShareWhatsApp)

        assertEquals(OrderSubmission.Idle, vm.uiState.value.submission)
        assertEquals(setOf(flaggedId), vm.uiState.value.unavailableIds)
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

        assertEquals(OrderSubmission.Error(), vm.uiState.value.submission)
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
        assertEquals(OrderSubmission.Error(), vm.uiState.value.submission)

        vm.onEvent(CartUiEvent.DismissError)

        assertEquals(OrderSubmission.Idle, vm.uiState.value.submission)
        assertFalse(repository.current.isEmpty)
    }

    // --- order-price-check ---

    private val sierra = CartFixtures.cartItem(productId = "p1", name = "Sierra", priceUnit = 210.0, quantity = 1)

    private fun FakeOrderRepository.failWith(category: OrderErrorCategory, details: OrderFailureDetails) {
        result = Result.failure(OrderException(category, details))
    }

    @Test
    fun opened_refreshesPricesAndShowsNotice() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(Cart(listOf(sierra)))
        val pricing = FakeCartPricingRepository().apply { answer(ProductSnapshot("p1", 199.0, "AVAILABLE", null)) }
        val vm = viewModel(repository, pricing = pricing)

        vm.onEvent(CartUiEvent.Opened)

        val state = vm.uiState.value
        assertEquals(199.0, state.totalAmount)
        assertEquals(listOf(CartPriceChange("p1", "Sierra", 210.0, 199.0)), state.notice?.priceChanges)
        assertEquals(1, pricing.calls)
    }

    @Test
    fun opened_refreshFailure_keepsBasketWithoutNotice() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(Cart(listOf(sierra)))
        val pricing = FakeCartPricingRepository(Result.failure(IllegalStateException("offline")))
        val vm = viewModel(repository, pricing = pricing)
        vm.enterValidData()

        vm.onEvent(CartUiEvent.Opened)

        assertEquals(210.0, vm.uiState.value.totalAmount)
        assertEquals(null, vm.uiState.value.notice)
        assertEquals("Ana Pérez", vm.uiState.value.name)
    }

    @Test
    fun opened_removesDeletedProductWithNotice() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(Cart(listOf(sierra)))
        val vm = viewModel(repository, pricing = FakeCartPricingRepository().apply { answer() })

        vm.onEvent(CartUiEvent.Opened)

        assertTrue(vm.uiState.value.isEmpty)
        assertEquals(listOf("Sierra"), vm.uiState.value.notice?.removedNames)
    }

    @Test
    fun placeOrder_waitsForRunningRefresh() = runTest(mainDispatcher.dispatcher) {
        val orderRepository = FakeOrderRepository()
        val pricing = store(Cart(listOf(sierra))).apply { gate = CompletableDeferred() }
        val vm = viewModel(FakeCartRepository(Cart(listOf(sierra))), orderRepository, pricing = pricing)
        vm.enterValidData()
        vm.onEvent(CartUiEvent.Opened)

        vm.onEvent(CartUiEvent.PlaceOrder)
        assertEquals(0, orderRepository.callCount)
        assertEquals(OrderSubmission.Submitting, vm.uiState.value.submission)

        pricing.gate!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, orderRepository.callCount)
        assertTrue(vm.uiState.value.submission is OrderSubmission.Success)
    }

    @Test
    fun placeOrder_refreshChangedDuringTap_sendsNothing() = runTest(mainDispatcher.dispatcher) {
        val orderRepository = FakeOrderRepository()
        val pricing = FakeCartPricingRepository().apply {
            answer(ProductSnapshot("p1", 199.0, "AVAILABLE", null))
            gate = CompletableDeferred()
        }
        val vm = viewModel(FakeCartRepository(Cart(listOf(sierra))), orderRepository, pricing = pricing)
        vm.enterValidData()
        vm.onEvent(CartUiEvent.Opened)

        vm.onEvent(CartUiEvent.PlaceOrder)
        pricing.gate!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(0, orderRepository.callCount)
        assertEquals(OrderSubmission.Idle, vm.uiState.value.submission)
        assertEquals(199.0, vm.uiState.value.totalAmount)
        assertEquals(1, vm.uiState.value.notice?.priceChanges?.size)
    }

    @Test
    fun placeOrder_priceChanged_updatesPricesAndReturnsToCart() = runTest(mainDispatcher.dispatcher) {
        val orderRepository = FakeOrderRepository().apply {
            failWith(
                OrderErrorCategory.PriceChanged,
                OrderFailureDetails(priceChanges = listOf(CartPriceChange("p1", "Sierra", 210.0, 199.0))),
            )
        }
        val vm = viewModel(FakeCartRepository(Cart(listOf(sierra))), orderRepository)
        vm.enterValidData()

        vm.onEvent(CartUiEvent.PlaceOrder)

        val state = vm.uiState.value
        assertEquals(OrderSubmission.Idle, state.submission) // not an error
        assertEquals(199.0, state.totalAmount)
        assertEquals(1, state.notice?.priceChanges?.size)
        assertEquals("Ana Pérez", state.name)

        orderRepository.result = null
        vm.onEvent(CartUiEvent.PlaceOrder)

        assertEquals(199.0, orderRepository.lastCall?.items?.single()?.priceUnit)
        assertTrue(vm.uiState.value.submission is OrderSubmission.Success)
    }

    @Test
    fun placeOrder_notSellable_flagsLineWithoutRecheck() = runTest(mainDispatcher.dispatcher) {
        val cart = CartFixtures.cart()
        val id = cart.items.first().productId
        val pricing = store(cart)
        val vm = viewModel(
            FakeCartRepository(cart),
            FakeOrderRepository().apply { failWith(OrderErrorCategory.ProductNotSellable, OrderFailureDetails(productId = id)) },
            pricing = pricing,
        )
        vm.enterValidData()

        vm.onEvent(CartUiEvent.PlaceOrder)

        assertEquals(OrderSubmission.Idle, vm.uiState.value.submission)
        assertEquals(setOf(id), vm.uiState.value.unavailableIds)
        assertEquals(0, pricing.calls)
    }

    @Test
    fun placeOrder_stockInsufficient_capsLine() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(Cart(listOf(sierra.copy(quantity = 5))))
        val vm = viewModel(
            repository,
            FakeOrderRepository().apply {
                failWith(OrderErrorCategory.StockInsufficient, OrderFailureDetails(productId = "p1", productName = "Sierra", available = 3))
            },
        )
        vm.enterValidData()

        vm.onEvent(CartUiEvent.PlaceOrder)

        assertEquals(OrderSubmission.Idle, vm.uiState.value.submission)
        assertEquals(3, repository.current.items.single().quantity)
        assertEquals(listOf(CartQuantityCap("p1", "Sierra", 3)), vm.uiState.value.notice?.capped)
    }

    @Test
    fun placeOrder_networkError_showsNetworkReason() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(
            FakeCartRepository(CartFixtures.cart()),
            FakeOrderRepository().apply { failWith(OrderErrorCategory.Network) },
        )
        vm.enterValidData()

        vm.onEvent(CartUiEvent.PlaceOrder)

        assertEquals(OrderSubmission.Error(OrderFailure.Network), vm.uiState.value.submission)
        assertFalse(vm.uiState.value.isEmpty) // basket kept
    }

    @Test
    fun retryOrder_afterNetworkError_createsOnce() = runTest(mainDispatcher.dispatcher) {
        val orderRepository = FakeOrderRepository().apply { failWith(OrderErrorCategory.Network) }
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), orderRepository)
        vm.enterValidData()
        vm.onEvent(CartUiEvent.PlaceOrder)

        orderRepository.result = null
        vm.onEvent(CartUiEvent.RetryOrder)
        vm.onEvent(CartUiEvent.RetryOrder) // a second tap on the success screen does nothing

        assertEquals(2, orderRepository.callCount)
        assertTrue(vm.uiState.value.submission is OrderSubmission.Success)
    }

    @Test
    fun placeOrder_timeout_offersNoRetry() = runTest(mainDispatcher.dispatcher) {
        val orderRepository = FakeOrderRepository().apply { failWith(OrderErrorCategory.Timeout) }
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()), orderRepository)
        vm.enterValidData()
        vm.onEvent(CartUiEvent.PlaceOrder)

        vm.onEvent(CartUiEvent.RetryOrder)

        assertEquals(OrderSubmission.Error(OrderFailure.Timeout), vm.uiState.value.submission)
        assertEquals(1, orderRepository.callCount)
    }

    @Test
    fun placeOrder_validation_showsField() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(
            FakeCartRepository(CartFixtures.cart()),
            FakeOrderRepository().apply {
                failWith(OrderErrorCategory.Validation, OrderFailureDetails(invalidFields = setOf(ConsumerField.Name)))
            },
        )
        vm.enterValidData()

        vm.onEvent(CartUiEvent.PlaceOrder)

        assertEquals(OrderSubmission.Error(OrderFailure.Validation(setOf(ConsumerField.Name))), vm.uiState.value.submission)
    }

    @Test
    fun success_showsServerTotal() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(FakeCartRepository(Cart(listOf(sierra.copy(quantity = 2)))))
        vm.enterValidData()

        vm.onEvent(CartUiEvent.PlaceOrder)

        val success = vm.uiState.value.submission as OrderSubmission.Success
        assertEquals(420.0, success.total)
        assertEquals(420.0, success.summary.single().lineTotal)
        assertFalse(success.totalMismatch)
    }

    @Test
    fun success_totalMismatch_flagged() = runTest(mainDispatcher.dispatcher) {
        val recorded = Order("o1", "PENDING", 199.0, 199.0, "Ana", "987654321", listOf(OrderItem("p1", "Sierra", 1, 199.0, 199.0)))
        val vm = viewModel(FakeCartRepository(Cart(listOf(sierra))), FakeOrderRepository(Result.success(recorded)))
        vm.enterValidData()

        vm.onEvent(CartUiEvent.PlaceOrder)

        val success = vm.uiState.value.submission as OrderSubmission.Success
        assertEquals(199.0, success.total)
        assertTrue(success.totalMismatch)
    }

    @Test
    fun share_networkFailure_showsNetworkCopy() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(
            FakeCartRepository(CartFixtures.cart()),
            shareRepository = FakeCartShareRepository(ShareOutcome.Failed(CartShareErrorCategory.Network)),
        )
        vm.enterValidData()

        vm.onEvent(CartUiEvent.ShareWhatsApp)
        advanceUntilIdle()

        assertEquals(OrderSubmission.ShareError(network = true), vm.uiState.value.submission)
    }

    @Test
    fun share_notSellable_flagsNamedLine() = runTest(mainDispatcher.dispatcher) {
        val cart = CartFixtures.cart()
        val id = cart.items.last().productId
        val pricing = store(cart)
        val vm = viewModel(
            FakeCartRepository(cart),
            shareRepository = FakeCartShareRepository(ShareOutcome.Rejected(ShareRejection.Unavailable, productId = id)),
            pricing = pricing,
        )
        vm.enterValidData()

        vm.onEvent(CartUiEvent.ShareWhatsApp)

        assertEquals(setOf(id), vm.uiState.value.unavailableIds)
        assertEquals(0, pricing.calls)
    }

    @Test
    fun name_truncatedAt200() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(FakeCartRepository(CartFixtures.cart()))

        vm.onEvent(CartUiEvent.NameChanged("A".repeat(262)))

        assertEquals(200, vm.uiState.value.name.length)
    }

    @Test
    fun notice_clearedOnQuantityChange() = runTest(mainDispatcher.dispatcher) {
        val repository = FakeCartRepository(Cart(listOf(sierra)))
        val vm = viewModel(repository, pricing = FakeCartPricingRepository().apply { answer(ProductSnapshot("p1", 199.0, "AVAILABLE", null)) })
        vm.onEvent(CartUiEvent.Opened)

        vm.onEvent(CartUiEvent.IncreaseLine("p1"))

        assertEquals(null, vm.uiState.value.notice)
    }
}
