package com.qhathia.mobile.discovery.feature.productdetail.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.CartConstants
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.domain.model.DetailErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.DetailException
import com.qhathia.mobile.discovery.core.domain.model.ProductDetail
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.domain.model.ShareOutcome
import com.qhathia.mobile.discovery.core.domain.model.toCartItem
import com.qhathia.mobile.discovery.core.domain.usecase.AddToCartResult
import com.qhathia.mobile.discovery.core.domain.usecase.AddToCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.EndCustomerSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GetProductDetailUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GetProductSuggestionsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.IsOrderableUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCustomerSessionEndUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ShareQuoteUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateConsumerNameUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ValidateConsumerPhoneUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.filterPhoneInput
import com.qhathia.mobile.discovery.core.ui.component.SubmissionLine
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiEffect
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiEvent
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiState
import com.qhathia.mobile.discovery.feature.productdetail.navigation.ARG_PRODUCT_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
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

/**
 * Drives the product-detail screen. Reads the product id from [SavedStateHandle], fetches the detail
 * via [GetProductDetailUseCase] on init (and on retry), and exposes the load status. Volver and the
 * confirmed Nueva búsqueda are surfaced as one-shot navigation effects.
 *
 * "Compartir por WhatsApp" opens a self-contained dialog that collects the consumer's name + phone
 * (validated with the same rules as the cart) and shares a quote PDF of the selected product via
 * [ShareQuoteUseCase] (which owns the retry policy), reusing the cart's loading/success/error
 * overlays. When the customer session ends (inactivity timeout), the dialog closes and its form is
 * cleared, so the next customer never sees the previous one's name or phone. The mic action and the mock related-products taps remain no-ops for this slice.
 */
@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProductDetail: GetProductDetailUseCase,
    private val getSuggestions: GetProductSuggestionsUseCase,
    observeCart: ObserveCartUseCase,
    private val addToCart: AddToCartUseCase,
    private val shareQuote: ShareQuoteUseCase,
    private val validateName: ValidateConsumerNameUseCase,
    private val validatePhone: ValidateConsumerPhoneUseCase,
    observeSessionEnd: ObserveCustomerSessionEndUseCase,
    private val isOrderable: IsOrderableUseCase,
    private val endCustomerSession: EndCustomerSessionUseCase,
) : ViewModel() {

    private val productId: String = savedStateHandle.get<String>(ARG_PRODUCT_ID).orEmpty()

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProductDetailUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<ProductDetailUiEffect> = _uiEffect.asSharedFlow()

    init {
        load()
        loadSuggestions()
        viewModelScope.launch {
            observeCart().collect { cart ->
                _uiState.update { it.copy(cartItemCount = cart.totalItemCount) }
            }
        }
        viewModelScope.launch {
            observeSessionEnd().collect { closeShareDialog() }
        }
    }

    fun onEvent(event: ProductDetailUiEvent) {
        when (event) {
            ProductDetailUiEvent.Retry -> retry()
            ProductDetailUiEvent.Back -> emit(ProductDetailUiEffect.NavigateBack)
            ProductDetailUiEvent.NewSearch -> _uiState.update { it.copy(showCloseConfirmDialog = true) }
            ProductDetailUiEvent.ConfirmCloseSearch -> confirmCloseSearch()
            ProductDetailUiEvent.DismissDialog -> _uiState.update { it.copy(showCloseConfirmDialog = false) }
            is ProductDetailUiEvent.QueryChanged -> _uiState.update { it.copy(query = event.query) }
            ProductDetailUiEvent.SubmitSearch -> onSubmitSearch()
            ProductDetailUiEvent.AddToCart -> onAddToCart()
            ProductDetailUiEvent.IncreaseQuantity -> adjustQuantity(delta = 1)
            ProductDetailUiEvent.DecreaseQuantity -> adjustQuantity(delta = -1)
            ProductDetailUiEvent.OpenCart -> emit(ProductDetailUiEffect.OpenCart)
            is ProductDetailUiEvent.RelatedProductTapped ->
                emit(ProductDetailUiEffect.NavigateToDetail(event.productId))
            is ProductDetailUiEvent.RelatedProductAddToCart -> onAddRelatedToCart(event.productId)
            ProductDetailUiEvent.ShareWhatsApp -> openShareDialog()
            is ProductDetailUiEvent.ShareNameChanged -> onShareNameChanged(event.name)
            is ProductDetailUiEvent.SharePhoneChanged -> onSharePhoneChanged(event.phone)
            ProductDetailUiEvent.ConfirmShare -> confirmShare()
            ProductDetailUiEvent.DismissShare -> if (!_uiState.value.share.isSubmitting) closeShareDialog()
            ProductDetailUiEvent.ShareDismissRequest -> onShareDismissRequest()
            ProductDetailUiEvent.AcceptShareSuccess -> closeShareDialog()
            ProductDetailUiEvent.DismissShareError -> updateShare { it.copy(submission = ProductDetailUiState.ShareSubmission.Idle) }
            // Voice is handed off to the search screen, which listens in place (wave 2, D4).
            ProductDetailUiEvent.ActivateMic -> emit(ProductDetailUiEffect.NavigateToSearch("", SearchSource.Voice))
        }
    }

    // --- Share-quote dialog ---

    /**
     * Opens the share dialog (fresh, empty form) only when a product detail is loaded and orderable:
     * the server rejects a share of an inactive or non-`AVAILABLE` product (DET-034).
     */
    private fun openShareDialog() {
        if (orderableDetail() == null) return
        updateShare { ProductDetailUiState.ShareUiState(visible = true) }
    }

    /**
     * Outside tap or system Back on the share dialog (wave 2, D6): a request in flight keeps it open,
     * and a result screen runs its main action ("Aceptar" closes, "Entendido" returns to the form).
     */
    private fun onShareDismissRequest() {
        when (_uiState.value.share.submission) {
            ProductDetailUiState.ShareSubmission.Submitting -> Unit
            ProductDetailUiState.ShareSubmission.Error, ProductDetailUiState.ShareSubmission.NotDelivered ->
                updateShare { it.copy(submission = ProductDetailUiState.ShareSubmission.Idle) }
            else -> closeShareDialog()
        }
    }

    /** Closes the dialog and resets its form so a re-open — or the next share — starts blank. */
    private fun closeShareDialog() {
        updateShare { ProductDetailUiState.ShareUiState() }
    }

    private fun onShareNameChanged(name: String) {
        val error = validateName(name)
        updateShare { it.copy(name = name, nameError = error, isShareEnabled = error == null && it.phoneError == null) }
    }

    private fun onSharePhoneChanged(raw: String) {
        val phone = filterPhoneInput(raw)
        val error = validatePhone(phone)
        updateShare { it.copy(phone = phone, phoneError = error, isShareEnabled = it.nameError == null && error == null) }
    }

    /**
     * Sends the quote PDF for the selected product (at the current stepper quantity). Guards valid data
     * and no in-flight submission. Only a delivered quote (`SENT`) shows the summary confirmation; a
     * quote WhatsApp did not receive shows [ProductDetailUiState.ShareSubmission.NotDelivered]; a
     * rejection or exhausted retries show the simplified error screen.
     */
    private fun confirmShare() {
        val detail = orderableDetail() ?: return
        val share = _uiState.value.share
        if (!share.isShareEnabled || share.submission != ProductDetailUiState.ShareSubmission.Idle) return

        val quantity = _uiState.value.quantity
        val item = detail.toCartItem(quantity)
        val summary = persistentListOf(SubmissionLine(imageUrl = item.imageUrl, name = item.name, quantity = quantity))

        updateShare { it.copy(submission = ProductDetailUiState.ShareSubmission.Submitting) }
        viewModelScope.launch {
            val submission = when (shareQuote(share.name, share.phone, listOf(item))) {
                is ShareOutcome.Delivered -> ProductDetailUiState.ShareSubmission.Success(summary)
                is ShareOutcome.NotDelivered -> ProductDetailUiState.ShareSubmission.NotDelivered
                is ShareOutcome.Rejected, is ShareOutcome.Failed -> ProductDetailUiState.ShareSubmission.Error
            }
            updateShare { it.copy(submission = submission) }
        }
    }

    private inline fun updateShare(transform: (ProductDetailUiState.ShareUiState) -> ProductDetailUiState.ShareUiState) {
        _uiState.update { it.copy(share = transform(it.share)) }
    }

    private fun onAddToCart() {
        val detail = orderableDetail() ?: return
        val quantity = _uiState.value.quantity
        // The stepper goes back to 1 once the units are in the cart, so a second tap adds one (DET-004).
        addAndReportLimit(detail.toCartItem(quantity)) { _uiState.update { it.copy(quantity = 1) } }
    }

    private fun onAddRelatedToCart(productId: String) {
        val related = _uiState.value.relatedProducts as? ProductDetailUiState.RelatedProducts.Loaded ?: return
        val product = related.items.firstOrNull { it.id == productId } ?: return
        addAndReportLimit(product.toCartItem())
    }

    private fun adjustQuantity(delta: Int) {
        val detail = orderableDetail() ?: return
        val max = CartConstants.maxQuantityFor(detail.availability)
        _uiState.update { it.copy(quantity = (it.quantity + delta).coerceIn(1, max)) }
    }

    private fun loadedDetail(): ProductDetail? =
        (_uiState.value.status as? ProductDetailUiState.Status.Loaded)?.detail

    /** The loaded detail when it can be added or shared; `null` for an inactive or non-AVAILABLE one. */
    private fun orderableDetail(): ProductDetail? =
        loadedDetail()?.takeIf { isOrderable(it.availability, it.active) }

    private fun onSubmitSearch() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return
        emit(ProductDetailUiEffect.NavigateToSearch(query))
    }

    private fun load() {
        _uiState.update { it.copy(status = ProductDetailUiState.Status.Loading) }
        viewModelScope.launch {
            getProductDetail(productId).fold(
                onSuccess = { detail ->
                    _uiState.update { it.copy(status = ProductDetailUiState.Status.Loaded(detail = detail)) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(status = ProductDetailUiState.Status.Error(error.toErrorKind())) }
                },
            )
        }
    }

    /**
     * Loads the related products independently of [load]: a non-empty success shows the strip; an empty
     * success or any failure collapses to the empty state, so a suggestions error never breaks the
     * detail screen.
     */
    /** "Reintentar": reloads the product and, unless they are already shown, its related products (RED-004). */
    private fun retry() {
        load()
        if (_uiState.value.relatedProducts !is ProductDetailUiState.RelatedProducts.Loaded) loadSuggestions()
    }

    private fun loadSuggestions() {
        _uiState.update { it.copy(relatedProducts = ProductDetailUiState.RelatedProducts.Loading) }
        viewModelScope.launch {
            val related = getSuggestions(productId)
                .map { products ->
                    if (products.isEmpty()) ProductDetailUiState.RelatedProducts.Empty
                    else ProductDetailUiState.RelatedProducts.Loaded(products.toImmutableList())
                }
                .getOrDefault(ProductDetailUiState.RelatedProducts.Empty)
            _uiState.update { it.copy(relatedProducts = related) }
        }
    }

    /** Confirmed close: end the customer session (basket + customer data, CAR-040), then go Home. */
    private fun confirmCloseSearch() {
        _uiState.update { it.copy(showCloseConfirmDialog = false) }
        viewModelScope.launch {
            endCustomerSession()
            _uiEffect.emit(ProductDetailUiEffect.NavigateHomeIdle)
        }
    }

    private fun emit(effect: ProductDetailUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    private fun Throwable.toErrorKind(): ProductDetailUiState.ErrorKind =
        when ((this as? DetailException)?.category ?: DetailErrorCategory.DetailApi) {
            DetailErrorCategory.NotFound -> ProductDetailUiState.ErrorKind.NotFound
            DetailErrorCategory.Network -> ProductDetailUiState.ErrorKind.Network
            DetailErrorCategory.DetailApi -> ProductDetailUiState.ErrorKind.Generic
        }

    /** Adds to the cart and tells the screen when the 20-product limit refused it (wave 2, D7). */
    private fun addAndReportLimit(item: CartItem, onAdded: () -> Unit = {}) {
        viewModelScope.launch {
            when (addToCart(item)) {
                AddToCartResult.Added -> onAdded()
                AddToCartResult.LineLimitReached -> emit(ProductDetailUiEffect.LineLimitReached)
                AddToCartResult.NotOrderable -> Unit
            }
        }
    }
}
