package com.qhathia.mobile.discovery.feature.productdetail.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.model.DetailErrorCategory
import com.qhathia.mobile.discovery.core.domain.model.DetailException
import com.qhathia.mobile.discovery.core.domain.usecase.GetProductDetailUseCase
import com.qhathia.mobile.discovery.feature.productdetail.data.RelatedProductsMock
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiEffect
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiEvent
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiState
import com.qhathia.mobile.discovery.feature.productdetail.navigation.ARG_PRODUCT_ID
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
 * Drives the product-detail screen. Reads the product id from [SavedStateHandle], fetches the detail
 * via [GetProductDetailUseCase] on init (and on retry), and exposes the load status. Volver and the
 * confirmed Nueva búsqueda are surfaced as one-shot navigation effects. The render-only actions
 * (add-to-cart, share, mic) and the mock related-products taps are intentional no-ops for this slice.
 */
@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProductDetail: GetProductDetailUseCase,
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
    }

    fun onEvent(event: ProductDetailUiEvent) {
        when (event) {
            ProductDetailUiEvent.Retry -> load()
            ProductDetailUiEvent.Back -> emit(ProductDetailUiEffect.NavigateBack)
            ProductDetailUiEvent.NewSearch -> _uiState.update { it.copy(showCloseConfirmDialog = true) }
            ProductDetailUiEvent.ConfirmCloseSearch -> confirmCloseSearch()
            ProductDetailUiEvent.DismissDialog -> _uiState.update { it.copy(showCloseConfirmDialog = false) }
            is ProductDetailUiEvent.QueryChanged -> _uiState.update { it.copy(query = event.query) }
            ProductDetailUiEvent.SubmitSearch -> onSubmitSearch()
            // Render-only / mock interactions — out of scope for this change.
            ProductDetailUiEvent.AddToCart,
            ProductDetailUiEvent.ShareWhatsApp,
            ProductDetailUiEvent.ActivateMic,
            is ProductDetailUiEvent.RelatedProductTapped,
            -> Unit
        }
    }

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
                    _uiState.update {
                        it.copy(
                            status = ProductDetailUiState.Status.Loaded(
                                detail = detail,
                                // PLACEHOLDER: mock related products until the related-products API lands.
                                relatedProducts = RelatedProductsMock.forProduct(detail.id).toImmutableList(),
                            ),
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(status = ProductDetailUiState.Status.Error(error.toErrorKind())) }
                },
            )
        }
    }

    private fun confirmCloseSearch() {
        _uiState.update { it.copy(showCloseConfirmDialog = false) }
        emit(ProductDetailUiEffect.NavigateHomeIdle)
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
}
