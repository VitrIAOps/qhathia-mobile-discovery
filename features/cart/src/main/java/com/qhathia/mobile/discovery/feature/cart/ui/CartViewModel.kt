package com.qhathia.mobile.discovery.feature.cart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.CartConstants
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.CartQuantityCap
import com.qhathia.mobile.discovery.core.domain.model.CartShareErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.Order
import com.qhathia.mobile.discovery.core.domain.model.OrderErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.OrderException
import com.qhathia.mobile.discovery.core.domain.model.OrderFailureDetails
import com.qhathia.mobile.discovery.core.domain.model.ShareOutcome
import com.qhathia.mobile.discovery.core.domain.model.ShareRejection
import com.qhathia.mobile.discovery.core.domain.model.toMoney
import com.qhathia.mobile.discovery.core.domain.usecase.ConsumerNameError
import com.qhathia.mobile.discovery.core.domain.usecase.ConsumerPhoneError
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
import com.qhathia.mobile.discovery.core.domain.usecase.filterPhoneInput
import com.qhathia.mobile.discovery.feature.cart.model.CartNotice
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEffect
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEvent
import com.qhathia.mobile.discovery.feature.cart.model.CartUiState
import com.qhathia.mobile.discovery.feature.cart.model.OrderFailure
import com.qhathia.mobile.discovery.feature.cart.model.OrderSubmission
import com.qhathia.mobile.discovery.feature.cart.model.OrderedLine
import com.qhathia.mobile.discovery.feature.cart.model.merge
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the cart dialog. Observes the session cart and maps it (with totals) into the UI state,
 * validates the consumer-data form on input, and adjusts line quantities / removes lines. Accepting
 * the success or cancelling the error ends the customer session through [EndCustomerSessionUseCase]
 * (basket **and** consumer form) and returns Home. Line taps and the close controls surface as
 * one-shot effects.
 *
 * This ViewModel is scoped to the Activity (the dialog is hosted outside the NavHost), so it outlives
 * one customer. It therefore resets its form and submission on every session end, including the
 * inactivity timeout the app shell runs (CAR-040/041).
 *
 * **Prices and stock (order-price-check).** Each time the dialog opens, the basket is refreshed against
 * the store in one request ([RefreshCartUseCase]); whatever changed on the customer's behalf (a price,
 * a removed product, a lowered quantity) is explained in [CartUiState.notice]. Orders carry the price
 * the customer saw, so a `409 PRICE_CHANGED` updates the prices and returns to the cart instead of
 * recording a total the customer never saw. "Realizar pedido" and "Compartir" wait for a refresh still
 * running and send nothing when it changed the basket.
 *
 * **Failures.** A rejection naming a product flags it (`PRODUCT_NOT_SELLABLE`) or lowers it to the
 * stock left (`STOCK_INSUFFICIENT`); one that does not name it rechecks the basket
 * ([RevalidateCartUseCase]). No connection offers "Reintentar"; a timeout never does (the order may
 * exist); a validation error names the field; anything else shows the simplified error.
 *
 * "Realizar pedido" creates the order and, on success, also shares the quote PDF to the consumer's
 * WhatsApp through [ShareQuoteUseCase] (which owns the retry policy). The success screen shows what the
 * server recorded (line totals and order total). "Compartir por WhatsApp" only shares the quote.
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
    private val refreshCart: RefreshCartUseCase,
    private val updatePrices: UpdateCartPricesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<CartUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<CartUiEffect> = _uiEffect.asSharedFlow()

    /** The opening refresh; completes with `true` when it changed the basket. */
    private var refreshJob: Deferred<Boolean>? = null

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
            is CartUiEvent.RemoveLine -> {
                clearNotice()
                viewModelScope.launch { removeItem(event.productId) }
            }
            is CartUiEvent.OpenLineDetail -> emit(CartUiEffect.NavigateToDetail(event.productId))
            is CartUiEvent.NameChanged -> onNameChanged(event.name)
            is CartUiEvent.PhoneChanged -> onPhoneChanged(event.phone)
            CartUiEvent.KeepShopping, CartUiEvent.Close -> if (!_uiState.value.isSubmitting) emit(CartUiEffect.Dismiss)
            CartUiEvent.DismissRequest -> onDismissRequest()
            CartUiEvent.PlaceOrder -> placeOrder()
            CartUiEvent.RetryOrder -> retryOrder()
            CartUiEvent.ShareWhatsApp -> share()
            CartUiEvent.AcceptSuccess, CartUiEvent.CancelOrder -> finishAndReturnHome()
            CartUiEvent.DismissError -> _uiState.update { it.copy(submission = OrderSubmission.Idle) }
            CartUiEvent.Opened -> onOpened()
        }
    }

    /**
     * The dialog was shown: refresh the basket against the store once. A failed refresh (no network)
     * leaves the basket and shows nothing.
     */
    private fun onOpened() {
        val state = _uiState.value
        if (state.isEmpty || state.submission != OrderSubmission.Idle) return
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.async {
            val refresh = refreshCart().getOrNull() ?: return@async false
            if (refresh.hasChanges) _uiState.update { it.copy(notice = it.notice.merge(CartNotice.of(refresh))) }
            refresh.hasChanges
        }
    }

    /**
     * Waits for an opening refresh that is still running. Returns `true` when it changed the basket,
     * in which case the caller sends nothing and returns to the cart: the customer must see the change
     * before confirming.
     */
    private suspend fun runningRefreshChangedBasket(): Boolean {
        val running = refreshJob?.takeIf { it.isActive } ?: return false
        return running.await()
    }

    /**
     * Outside tap or system Back (wave 2, D6: CAR-043, CAR-044). A request in flight keeps the dialog
     * open; a result is never left behind for the next time the cart opens: success is accepted (the
     * session ends as with "Aceptar") and an error returns to the cart summary as with "Entendido".
     */
    private fun onDismissRequest() {
        when (_uiState.value.submission) {
            OrderSubmission.Submitting -> Unit
            is OrderSubmission.Success -> finishAndReturnHome()
            is OrderSubmission.Error, is OrderSubmission.ShareError, OrderSubmission.QuoteNotDelivered ->
                _uiState.update { it.copy(submission = OrderSubmission.Idle) }
            OrderSubmission.Idle -> emit(CartUiEffect.Dismiss)
        }
    }

    /**
     * "Realizar pedido": creates the order and, on success, shares the quote PDF too. The success
     * screen shows what the server recorded; `shareDelivered` is `true` only for
     * [ShareOutcome.Delivered], so a quote that never lands still confirms the order (fallback copy).
     */
    private fun placeOrder() {
        if (!beginSubmission()) return
        viewModelScope.launch {
            if (runningRefreshChangedBasket()) {
                backToCart()
                return@launch
            }
            val state = _uiState.value
            createOrder(state.name, state.phone, state.items)
                .onSuccess { order ->
                    val delivered = shareQuote(state.name, state.phone, state.items) is ShareOutcome.Delivered
                    _uiState.update { it.copy(submission = order.toSuccess(state, delivered)) }
                }
                .onFailure { cause -> onOrderFailure(cause as? OrderException, state.items) }
        }
    }

    /** "Reintentar" after a connection error: one more attempt through the same guarded path. */
    private fun retryOrder() {
        val submission = _uiState.value.submission
        if (submission !is OrderSubmission.Error || submission.reason != OrderFailure.Network) return
        _uiState.update { it.copy(submission = OrderSubmission.Idle) }
        placeOrder()
    }

    private suspend fun onOrderFailure(failure: OrderException?, items: List<CartItem>) {
        val details = failure?.details ?: OrderFailureDetails()
        when (failure?.category) {
            OrderErrorCategory.PriceChanged -> {
                updatePrices(details.priceChanges)
                backToCart(CartNotice(priceChanges = details.priceChanges))
            }
            OrderErrorCategory.ProductNotSellable ->
                flagNamedLine(details.productId, items) ?: flagUnavailableLines(items)
            OrderErrorCategory.StockInsufficient ->
                lowerToStock(details.productId, details.productName, details.available, items) ?: flagUnavailableLines(items)
            OrderErrorCategory.Unavailable, OrderErrorCategory.NotFound -> flagUnavailableLines(items)
            OrderErrorCategory.Network -> showError(OrderFailure.Network)
            OrderErrorCategory.Timeout -> showError(OrderFailure.Timeout)
            OrderErrorCategory.Validation -> showError(OrderFailure.Validation(details.invalidFields))
            OrderErrorCategory.OrderApi, null -> showError(OrderFailure.Generic)
        }
    }

    /**
     * "Compartir por WhatsApp": shares the quote PDF only (no order). [ShareOutcome.Delivered] resolves
     * to the quote-only [OrderSubmission.Success]; [ShareOutcome.NotDelivered] to
     * [OrderSubmission.QuoteNotDelivered]; a rejection naming a product flags or lowers that line; any
     * other failure resolves to [OrderSubmission.ShareError], saying so when the network was the cause.
     */
    private fun share() {
        if (!beginSubmission()) return
        viewModelScope.launch {
            if (runningRefreshChangedBasket()) {
                backToCart()
                return@launch
            }
            val state = _uiState.value
            val outcome = shareQuote(state.name, state.phone, state.items)
            if (outcome is ShareOutcome.Rejected && onShareRejected(outcome, state.items)) return@launch
            val submission = when (outcome) {
                is ShareOutcome.Delivered -> OrderSubmission.Success(state.summary(), isQuoteOnly = true)
                is ShareOutcome.NotDelivered -> OrderSubmission.QuoteNotDelivered
                is ShareOutcome.Failed -> OrderSubmission.ShareError(network = outcome.category == CartShareErrorCategory.Network)
                is ShareOutcome.Rejected -> OrderSubmission.ShareError()
            }
            _uiState.update { it.copy(submission = submission) }
        }
    }

    /** Handles the product rejections of a share; returns `true` when the cart already shows the result. */
    private suspend fun onShareRejected(outcome: ShareOutcome.Rejected, items: List<CartItem>): Boolean =
        when (outcome.kind) {
            ShareRejection.Unavailable -> {
                flagNamedLine(outcome.productId, items) ?: flagUnavailableLines(items, fallback = OrderSubmission.ShareError())
                true
            }
            ShareRejection.NotFound -> {
                flagUnavailableLines(items, fallback = OrderSubmission.ShareError())
                true
            }
            ShareRejection.StockInsufficient ->
                lowerToStock(outcome.productId, outcome.productName, outcome.available, items) != null
            ShareRejection.Invalid, ShareRejection.Other -> false
        }

    /**
     * Guards valid data and no in-flight/resolved submission (blocks double-submit and cross-action
     * overlap between the two buttons), clears the notice and flips the phase to
     * [OrderSubmission.Submitting]. Returns `false` when the submission must not start.
     */
    private fun beginSubmission(): Boolean {
        val state = _uiState.value
        if (!state.canSubmit || state.submission != OrderSubmission.Idle) return false
        _uiState.update { it.copy(submission = OrderSubmission.Submitting, notice = null) }
        return true
    }

    private fun showError(reason: OrderFailure) {
        _uiState.update { it.copy(submission = OrderSubmission.Error(reason)) }
    }

    /** Back to the cart with [notice] added (the basket already changed); not an error. */
    private fun backToCart(notice: CartNotice? = null) {
        _uiState.update {
            it.copy(submission = OrderSubmission.Idle, notice = if (notice == null) it.notice else it.notice.merge(notice))
        }
    }

    /** Flags [productId] when it is in the basket. Returns `null` when it is not, so the caller rechecks. */
    private fun flagNamedLine(productId: String?, items: List<CartItem>): Unit? {
        if (productId == null || items.none { it.productId == productId }) return null
        _uiState.update {
            it.copy(submission = OrderSubmission.Idle, unavailableIds = (it.unavailableIds + productId).toImmutableSet())
        }
        return Unit
    }

    /**
     * Lowers [productId] to the [available] units (0 removes it) and explains it. Returns `null` when
     * the answer did not name a product in the basket, so the caller rechecks.
     */
    private suspend fun lowerToStock(productId: String?, productName: String?, available: Int?, items: List<CartItem>): Unit? {
        val line = items.firstOrNull { it.productId == productId } ?: return null
        val units = available ?: return null
        updateQuantity(line.productId, units)
        val name = productName ?: line.name
        backToCart(
            if (units > 0) CartNotice(capped = listOf(CartQuantityCap(line.productId, name, units)))
            else CartNotice(removedNames = listOf(name)),
        )
        return Unit
    }

    /**
     * After a rejection that does not name the product: rechecks [items] against the store (the loader
     * stays up meanwhile). Flagged lines return the consumer to the cart with the "No disponible" marks;
     * when nothing can be blamed, [fallback] is shown.
     */
    private suspend fun flagUnavailableLines(
        items: List<CartItem>,
        fallback: OrderSubmission = OrderSubmission.Error(),
    ) {
        val flagged = revalidateCart(items)
        _uiState.update {
            if (flagged.isEmpty()) {
                it.copy(submission = fallback)
            } else {
                it.copy(submission = OrderSubmission.Idle, unavailableIds = flagged.toImmutableSet())
            }
        }
    }

    /** The success screen from what the server recorded; images come from the basket snapshot. */
    private fun Order.toSuccess(state: CartUiState, shareDelivered: Boolean): OrderSubmission.Success {
        val images = state.items.associate { it.productId to it.imageUrl }
        return OrderSubmission.Success(
            summary = items.map {
                OrderedLine(
                    imageUrl = images[it.productId].orEmpty(),
                    name = it.productName,
                    quantity = it.quantity,
                    lineTotal = it.lineTotal,
                )
            }.toImmutableList(),
            shareDelivered = shareDelivered,
            total = total,
            totalMismatch = total.toMoney() != state.totalAmount.toMoney(),
        )
    }

    /** Snapshots the basket lines for a quote confirmation, so it survives the basket being cleared. */
    private fun CartUiState.summary(): ImmutableList<OrderedLine> =
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
        refreshJob?.cancel()
        refreshJob = null
        _uiState.update {
            it.copy(
                submission = OrderSubmission.Idle,
                name = "",
                phone = "",
                nameError = ConsumerNameError.Empty,
                phoneError = ConsumerPhoneError.Empty,
                isOrderEnabled = false,
                unavailableIds = persistentSetOf(),
                notice = null,
            )
        }
    }

    private fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    private fun changeQuantity(productId: String, delta: Int) {
        val item = _uiState.value.items.firstOrNull { it.productId == productId } ?: return
        clearNotice()
        viewModelScope.launch { updateQuantity(productId, item.quantity + delta) }
    }

    /** The server accepts at most [CartConstants.MAX_CONSUMER_NAME_CHARS]: longer input is not entered. */
    private fun onNameChanged(raw: String) {
        val name = raw.take(CartConstants.MAX_CONSUMER_NAME_CHARS)
        val error = validateName(name)
        _uiState.update { it.copy(name = name, nameError = error, isOrderEnabled = error == null && it.phoneError == null) }
    }

    private fun onPhoneChanged(raw: String) {
        val phone = filterPhoneInput(raw)
        val error = validatePhone(phone)
        _uiState.update { it.copy(phone = phone, phoneError = error, isOrderEnabled = it.nameError == null && error == null) }
    }

    private fun emit(effect: CartUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
