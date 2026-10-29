package com.qhathia.mobile.discovery.feature.cart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.OrderErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.OrderException
import com.qhathia.mobile.discovery.core.domain.model.ShareOutcome
import com.qhathia.mobile.discovery.core.domain.model.ShareRejection
import com.qhathia.mobile.discovery.core.domain.usecase.CreateOrderUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.EndCustomerSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCustomerSessionEndUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RemoveCartItemUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RevalidateCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ConsumerNameError
import com.qhathia.mobile.discovery.core.domain.usecase.ConsumerPhoneError
import com.qhathia.mobile.discovery.core.domain.usecase.ShareQuoteUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.UpdateCartItemQuantityUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateConsumerNameUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateConsumerPhoneUseCase
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEffect
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEvent
import com.qhathia.mobile.discovery.feature.cart.model.CartUiState
import com.qhathia.mobile.discovery.feature.cart.model.OrderSubmission
import com.qhathia.mobile.discovery.feature.cart.model.OrderedLine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the cart dialog. Observes the session cart and maps it (with totals) into the UI state,
 * validates the consumer-data form on input, and adjusts line quantities / removes lines. "Realizar
 * pedido" submits the basket + consumer data to the order API and drives the submission phase
 * ([OrderSubmission]): a blocking loader while in flight, a success summary on `201`, or a simplified
 * error otherwise. Accepting the success or cancelling the error ends the customer session through
 * [EndCustomerSessionUseCase] (basket **and** consumer form) and returns Home; "Entendido" returns to
 * the cart summary. Line taps and the close controls surface as one-shot effects.
 *
 * This ViewModel is scoped to the Activity (the dialog is hosted outside the NavHost), so it outlives
 * one customer. It therefore resets its form and submission on every session end, including the
 * inactivity timeout the app shell runs (CAR-040/041).
 *
 * When the server rejects an order or share because a product is no longer available (`400` / `404`),
 * [RevalidateCartUseCase] rechecks the lines. The unavailable ones are flagged "No disponible" and the
 * actions stay blocked until they are removed, so the same basket is never resent (CAR-023/025).
 *
 * "Realizar pedido" creates the order and, on success, also shares the quote PDF to the consumer's
 * WhatsApp through [ShareQuoteUseCase] (which owns the retry policy). The order is confirmed either
 * way; unless the quote was delivered (`SENT`), the confirmation switches to the "go to the seller and
 * give your name" fallback copy. "Compartir por WhatsApp" only shares the quote, without creating an
 * order: a delivered quote shows the quote-sent success, a quote WhatsApp did not receive shows
 * [OrderSubmission.QuoteNotDelivered], and any other failure shows the generic error.
 */
@HiltViewModel
class CartViewModel @Inject constructor(
    observeCart: ObserveCartUseCase,
    private val updateQuantity: UpdateCartItemQuantityUseCase,
    private val removeItem: RemoveCartItemUseCase,
    private val validateName: ValidateConsumerNameUseCase,
    private val validatePhone: ValidateConsumerPhoneUseCase,
    private val createOrder: CreateOrderUseCase,
    private val shareQuote: ShareQuoteUseCase,
    private val endCustomerSession: EndCustomerSessionUseCase,
    observeSessionEnd: ObserveCustomerSessionEndUseCase,
    private val revalidateCart: RevalidateCartUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<CartUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<CartUiEffect> = _uiEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            observeCart().collect { cart ->
                _uiState.update {
                    it.copy(
                        items = cart.items.toImmutableList(),
                        totalItemCount = cart.totalItemCount,
                        totalAmount = cart.totalAmount,
                        // Forget flags of lines that left the basket.
                        unavailableIds = it.unavailableIds
                            .filterTo(mutableSetOf()) { id -> cart.items.any { item -> item.productId == id } }
                            .toImmutableSet(),
                    )
                }
            }
        }
        viewModelScope.launch {
            observeSessionEnd().collect { resetCustomerData() }
        }
    }

    fun onEvent(event: CartUiEvent) {
        when (event) {
            is CartUiEvent.IncreaseLine -> changeQuantity(event.productId, delta = 1)
            is CartUiEvent.DecreaseLine -> changeQuantity(event.productId, delta = -1)
            is CartUiEvent.RemoveLine -> viewModelScope.launch { removeItem(event.productId) }
            is CartUiEvent.OpenLineDetail -> emit(CartUiEffect.NavigateToDetail(event.productId))
            is CartUiEvent.NameChanged -> onNameChanged(event.name)
            is CartUiEvent.PhoneChanged -> onPhoneChanged(event.phone)
            CartUiEvent.KeepShopping, CartUiEvent.Close -> emit(CartUiEffect.Dismiss)
            CartUiEvent.PlaceOrder -> placeOrder()
            CartUiEvent.ShareWhatsApp -> share()
            CartUiEvent.AcceptSuccess, CartUiEvent.CancelOrder -> finishAndReturnHome()
            CartUiEvent.DismissError -> _uiState.update { it.copy(submission = OrderSubmission.Idle) }
        }
    }

    /**
     * "Realizar pedido": creates the order and, on success, shares the quote PDF too. An order failure
     * resolves to [OrderSubmission.Error]; an order success resolves to [OrderSubmission.Success] whose
     * `shareDelivered` is `true` only for [ShareOutcome.Delivered] — so a quote that never lands still
     * confirms the order (with the fallback copy).
     */
    private fun placeOrder() {
        val state = beginSubmission() ?: return
        val summary = state.summary()
        viewModelScope.launch {
            createOrder(state.name, state.phone, state.items)
                .onSuccess {
                    val delivered = shareQuote(state.name, state.phone, state.items) is ShareOutcome.Delivered
                    _uiState.update {
                        it.copy(submission = OrderSubmission.Success(summary, shareDelivered = delivered))
                    }
                }
                .onFailure { cause ->
                    when ((cause as? OrderException)?.category) {
                        OrderErrorCategory.Unavailable, OrderErrorCategory.NotFound -> flagUnavailableLines(state.items)
                        else -> _uiState.update { it.copy(submission = OrderSubmission.Error) }
                    }
                }
        }
    }

    /**
     * "Compartir por WhatsApp": shares the quote PDF only (no order). [ShareOutcome.Delivered] resolves
     * to the quote-only [OrderSubmission.Success]; [ShareOutcome.NotDelivered] to
     * [OrderSubmission.QuoteNotDelivered]; a rejection or exhausted retries to [OrderSubmission.Error].
     */
    private fun share() {
        val state = beginSubmission() ?: return
        val summary = state.summary()
        viewModelScope.launch {
            val outcome = shareQuote(state.name, state.phone, state.items)
            if (outcome.isUnavailableRejection()) {
                flagUnavailableLines(state.items)
                return@launch
            }
            val submission = when (outcome) {
                is ShareOutcome.Delivered -> OrderSubmission.Success(summary, isQuoteOnly = true)
                is ShareOutcome.NotDelivered -> OrderSubmission.QuoteNotDelivered
                is ShareOutcome.Rejected, is ShareOutcome.Failed -> OrderSubmission.Error
            }
            _uiState.update { it.copy(submission = submission) }
        }
    }

    /**
     * Guards valid data and no in-flight/resolved submission (blocks double-submit and cross-action
     * overlap between the two buttons), flips the phase to [OrderSubmission.Submitting], and returns
     * the guarded snapshot — or `null` when the submission must not start.
     */
    private fun beginSubmission(): CartUiState? {
        val state = _uiState.value
        if (!state.canSubmit || state.submission != OrderSubmission.Idle) return null
        _uiState.update { it.copy(submission = OrderSubmission.Submitting) }
        return state
    }

    /**
     * After a `400` / `404`: rechecks [items] (the loader stays up meanwhile). Flagged lines return the
     * consumer to the cart with the "No disponible" marks; when nothing can be blamed, the generic
     * error is shown as before.
     */
    private suspend fun flagUnavailableLines(items: List<CartItem>) {
        val flagged = revalidateCart(items)
        _uiState.update {
            if (flagged.isEmpty()) {
                it.copy(submission = OrderSubmission.Error)
            } else {
                it.copy(submission = OrderSubmission.Idle, unavailableIds = flagged.toImmutableSet())
            }
        }
    }

    private fun ShareOutcome.isUnavailableRejection(): Boolean =
        this is ShareOutcome.Rejected &&
            (kind == ShareRejection.Unavailable || kind == ShareRejection.NotFound)

    /** Snapshots the basket lines for the confirmation summary, so it survives the basket being cleared. */
    private fun CartUiState.summary(): kotlinx.collections.immutable.ImmutableList<OrderedLine> =
        items.map { OrderedLine(imageUrl = it.imageUrl, name = it.name, quantity = it.quantity) }
            .toImmutableList()

    private fun finishAndReturnHome() {
        viewModelScope.launch {
            endCustomerSession()
            // The session-end collector resets too; resetting here keeps the order deterministic.
            resetCustomerData()
            _uiEffect.emit(CartUiEffect.ReturnHome)
        }
    }

    /**
     * Resets the phase and clears the consumer form so a reopened cart is fresh — never stuck on the
     * previous success/error screen nor pre-filled with the last consumer's data.
     */
    private fun resetCustomerData() {
        _uiState.update {
            it.copy(
                submission = OrderSubmission.Idle,
                name = "",
                phone = "",
                nameError = ConsumerNameError.Empty,
                phoneError = ConsumerPhoneError.Empty,
                isOrderEnabled = false,
                unavailableIds = persistentSetOf(),
            )
        }
    }

    private fun changeQuantity(productId: String, delta: Int) {
        val item = _uiState.value.items.firstOrNull { it.productId == productId } ?: return
        viewModelScope.launch { updateQuantity(productId, item.quantity + delta) }
    }

    private fun onNameChanged(name: String) {
        val error = validateName(name)
        _uiState.update { it.copy(name = name, nameError = error, isOrderEnabled = error == null && it.phoneError == null) }
    }

    private fun onPhoneChanged(phone: String) {
        val error = validatePhone(phone)
        _uiState.update { it.copy(phone = phone, phoneError = error, isOrderEnabled = it.nameError == null && error == null) }
    }

    private fun emit(effect: CartUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
