package com.qhathia.mobile.discovery.feature.cart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.usecase.ClearCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.CreateOrderUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.RemoveCartItemUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.UpdateCartItemQuantityUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateConsumerNameUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateConsumerPhoneUseCase
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEffect
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEvent
import com.qhathia.mobile.discovery.feature.cart.model.CartUiState
import com.qhathia.mobile.discovery.feature.cart.model.OrderSubmission
import com.qhathia.mobile.discovery.feature.cart.model.OrderedLine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
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
 * error otherwise. Accepting the success or cancelling the error clears the session and returns Home;
 * "Entendido" returns to the cart summary. Line taps and the close controls surface as one-shot
 * effects. "Compartir por WhatsApp" remains an intentional no-op (its API does not exist yet).
 */
@HiltViewModel
class CartViewModel @Inject constructor(
    observeCart: ObserveCartUseCase,
    private val updateQuantity: UpdateCartItemQuantityUseCase,
    private val removeItem: RemoveCartItemUseCase,
    private val validateName: ValidateConsumerNameUseCase,
    private val validatePhone: ValidateConsumerPhoneUseCase,
    private val createOrder: CreateOrderUseCase,
    private val clearCart: ClearCartUseCase,
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
                    )
                }
            }
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
            CartUiEvent.AcceptSuccess, CartUiEvent.CancelOrder -> finishAndReturnHome()
            CartUiEvent.DismissError -> _uiState.update { it.copy(submission = OrderSubmission.Idle) }
            // TODO(whatsapp-api): build and send the WhatsApp message when the share contract lands.
            CartUiEvent.ShareWhatsApp -> Unit
        }
    }

    private fun placeOrder() {
        val state = _uiState.value
        // Guard: valid data and no submission already in flight (blocks double-submit).
        if (!state.isOrderEnabled || state.submission != OrderSubmission.Idle) return

        val summary = state.items
            .map { OrderedLine(imageUrl = it.imageUrl, name = it.name, quantity = it.quantity) }
            .toImmutableList()

        _uiState.update { it.copy(submission = OrderSubmission.Submitting) }
        viewModelScope.launch {
            createOrder(state.name, state.phone, state.items)
                .onSuccess {
                    _uiState.update { it.copy(submission = OrderSubmission.Success(summary)) }
                }
                .onFailure {
                    _uiState.update { it.copy(submission = OrderSubmission.Error) }
                }
        }
    }

    private fun finishAndReturnHome() {
        viewModelScope.launch {
            clearCart()
            _uiEffect.emit(CartUiEffect.ReturnHome)
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
