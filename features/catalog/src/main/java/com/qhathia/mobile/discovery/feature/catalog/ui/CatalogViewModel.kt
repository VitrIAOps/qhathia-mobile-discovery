package com.qhathia.mobile.discovery.feature.catalog.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.CatalogConstants
import com.qhathia.mobile.discovery.core.domain.model.CatalogCategory
import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.domain.model.toCartItem
import com.qhathia.mobile.discovery.core.domain.usecase.AddToCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GetProductCatalogUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCatalogRefreshUseCase
import com.qhathia.mobile.discovery.feature.catalog.model.CatalogTab
import com.qhathia.mobile.discovery.feature.catalog.model.CatalogUiEffect
import com.qhathia.mobile.discovery.feature.catalog.model.CatalogUiEvent
import com.qhathia.mobile.discovery.feature.catalog.model.CatalogUiState
import com.qhathia.mobile.discovery.feature.catalog.navigation.ARG_SOURCE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
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
 * Drives the catalog screen: loads the store's products grouped by category, builds the category tab
 * bar (Todo + one tab per category + Descuentos), and filters a single two-row product grid by the
 * selected tab. Observes the cart for the badge and adds products to the cart. The bottom search bar
 * hands off to the product-search flow (text submit and mic). A load failure shows the connection-lost
 * screen, which returns Home idle after 30s of inactivity.
 *
 * A remote "Actualizar catálogo" (see [ObserveCatalogRefreshUseCase]) re-fetches the catalog in the
 * background and swaps in the fresh products without a spinner, keeping the selected tab.
 *
 * The catalog has no multi-step internal history, so Volver either opens the close-confirmation dialog
 * (when the cart has items) or returns Home idle (empty cart).
 */
@HiltViewModel
class CatalogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProductCatalog: GetProductCatalogUseCase,
    observeCart: ObserveCartUseCase,
    private val addToCart: AddToCartUseCase,
    observeCatalogRefresh: ObserveCatalogRefreshUseCase,
) : ViewModel() {

    private val source: SearchSource = runCatching {
        SearchSource.valueOf(savedStateHandle.get<String>(ARG_SOURCE) ?: SearchSource.Text.name)
    }.getOrDefault(SearchSource.Text)

    private val _uiState = MutableStateFlow(CatalogUiState(source = source))
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<CatalogUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<CatalogUiEffect> = _uiEffect.asSharedFlow()

    /** The loaded categories, kept so tab selection can re-filter the grid without re-fetching. */
    private var categories: List<CatalogCategory> = emptyList()

    private var loadJob: Job? = null
    private var refreshJob: Job? = null
    private var idleJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            observeCart().collect { cart ->
                _uiState.update { it.copy(cartItemCount = cart.totalItemCount) }
            }
        }
        viewModelScope.launch {
            observeCatalogRefresh().collect { refreshInPlace() }
        }
    }

    fun onEvent(event: CatalogUiEvent) {
        when (event) {
            CatalogUiEvent.Retry -> load()
            is CatalogUiEvent.TabSelected -> selectTab(event.tabId)
            is CatalogUiEvent.ProductTapped -> openDetail(event.productId)
            is CatalogUiEvent.CartTapped -> onCartTapped(event.productId)
            CatalogUiEvent.OpenCart -> openCart()
            CatalogUiEvent.Back -> onBack()
            CatalogUiEvent.NewSearch -> _uiState.update { it.copy(showCloseConfirmDialog = true) }
            CatalogUiEvent.ConfirmCloseSearch -> confirmClose()
            CatalogUiEvent.DismissDialog -> _uiState.update { it.copy(showCloseConfirmDialog = false) }
            is CatalogUiEvent.QueryChanged -> onQueryChanged(event.query)
            CatalogUiEvent.SubmitSearch -> onSubmitSearch()
            CatalogUiEvent.ActivateMic -> onActivateMic()
        }
    }

    private fun load() {
        cancelIdleTimer()
        loadJob?.cancel()
        refreshJob?.cancel()
        _uiState.update { it.copy(isLoading = true, isNetworkError = false) }
        loadJob = viewModelScope.launch {
            getProductCatalog().fold(
                onSuccess = { settleCatalog(it) },
                onFailure = { settleError() },
            )
        }
    }

    /**
     * Remote refresh: a full load already in flight is fresh enough; on the error screen it is a retry;
     * otherwise fetch silently and keep the current products if that fails.
     */
    private fun refreshInPlace() {
        val state = _uiState.value
        when {
            state.isLoading -> Unit
            state.isNetworkError -> load()
            else -> {
                refreshJob?.cancel()
                refreshJob = viewModelScope.launch {
                    getProductCatalog().onSuccess { settleRefreshed(it) }
                }
            }
        }
    }

    private fun settleRefreshed(loaded: List<CatalogCategory>) {
        categories = loaded
        val tabs = buildTabs(loaded)
        val selected = _uiState.value.selectedTabId.takeIf { id -> tabs.any { it.id == id } } ?: CatalogTab.ALL_ID
        _uiState.update {
            it.copy(tabs = tabs, selectedTabId = selected, visibleProducts = productsFor(selected))
        }
    }

    private fun settleCatalog(loaded: List<CatalogCategory>) {
        categories = loaded
        val tabs = buildTabs(loaded)
        _uiState.update {
            it.copy(
                isLoading = false,
                isNetworkError = false,
                tabs = tabs,
                selectedTabId = CatalogTab.ALL_ID,
                visibleProducts = productsFor(CatalogTab.ALL_ID),
            )
        }
    }

    private fun settleError() {
        categories = emptyList()
        _uiState.update {
            it.copy(isLoading = false, isNetworkError = true, tabs = persistentListOf(), visibleProducts = persistentListOf())
        }
        startIdleTimer()
    }

    /** Builds the tab bar: Todo first, one tab per category, Descuentos last. Empty when no categories. */
    private fun buildTabs(loaded: List<CatalogCategory>): ImmutableList<CatalogTab> {
        if (loaded.isEmpty()) return persistentListOf()
        val tabs = buildList {
            add(CatalogTab(id = CatalogTab.ALL_ID, kind = CatalogTab.Kind.All))
            loaded.forEach { category ->
                add(CatalogTab(id = CatalogTab.categoryId(category.id), kind = CatalogTab.Kind.Category, label = category.name))
            }
            add(CatalogTab(id = CatalogTab.DISCOUNTS_ID, kind = CatalogTab.Kind.Discounts))
        }
        return tabs.toImmutableList()
    }

    private fun selectTab(tabId: String) {
        resetIdleTimerIfError()
        if (_uiState.value.selectedTabId == tabId) return
        _uiState.update { it.copy(selectedTabId = tabId, visibleProducts = productsFor(tabId)) }
    }

    /** The products to show for [tabId]: all products, a single category's, or the discounted ones. */
    private fun productsFor(tabId: String): ImmutableList<Product> {
        val products = when (tabId) {
            CatalogTab.ALL_ID -> categories.flatMap { it.products }
            CatalogTab.DISCOUNTS_ID -> categories.flatMap { it.products }.filter(CatalogConstants::isDiscountCandidate)
            else -> categories.firstOrNull { CatalogTab.categoryId(it.id) == tabId }?.products.orEmpty()
        }
        return products.distinctBy { it.id }.toImmutableList()
    }

    private fun openDetail(productId: String) {
        resetIdleTimerIfError()
        emit(CatalogUiEffect.NavigateToDetail(productId))
    }

    private fun onCartTapped(productId: String) {
        resetIdleTimerIfError()
        val product = currentProducts().firstOrNull { it.id == productId } ?: return
        viewModelScope.launch { addToCart(product.toCartItem()) }
    }

    private fun openCart() {
        resetIdleTimerIfError()
        emit(CatalogUiEffect.OpenCart)
    }

    /** Distinct products across the whole catalog, for cart adds (independent of the selected tab). */
    private fun currentProducts(): List<Product> = categories.flatMap { it.products }.distinctBy { it.id }

    private fun onBack() {
        // No internal history: with items in the cart confirm the close, otherwise return Home directly.
        if (_uiState.value.cartItemCount > 0) {
            _uiState.update { it.copy(showCloseConfirmDialog = true) }
        } else {
            confirmClose()
        }
    }

    private fun confirmClose() {
        cancelIdleTimer()
        loadJob?.cancel()
        _uiState.update { it.copy(showCloseConfirmDialog = false) }
        emit(CatalogUiEffect.NavigateHomeIdle)
    }

    private fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query.take(CatalogConstants.MAX_QUERY_CHARS)) }
        resetIdleTimerIfError()
    }

    private fun onSubmitSearch() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return
        emit(CatalogUiEffect.NavigateToSearch(query, SearchSource.Text))
    }

    private fun onActivateMic() {
        // The catalog hands voice off to the product-search flow, which owns in-place voice capture.
        emit(CatalogUiEffect.NavigateToSearch(_uiState.value.query.trim(), SearchSource.Voice))
    }

    private fun startIdleTimer() {
        idleJob?.cancel()
        idleJob = viewModelScope.launch {
            delay(CatalogConstants.ERROR_IDLE_TIMEOUT_SECONDS * ONE_SECOND_MS)
            emit(CatalogUiEffect.NavigateHomeIdle)
        }
    }

    private fun resetIdleTimerIfError() {
        if (_uiState.value.isNetworkError) startIdleTimer()
    }

    private fun cancelIdleTimer() {
        idleJob?.cancel()
        idleJob = null
    }

    private fun emit(effect: CatalogUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    private companion object {
        const val ONE_SECOND_MS = 1_000L
    }
}
