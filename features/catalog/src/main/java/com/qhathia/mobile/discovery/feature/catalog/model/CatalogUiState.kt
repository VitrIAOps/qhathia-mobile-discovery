package com.qhathia.mobile.discovery.feature.catalog.model

import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable snapshot of the catalog screen.
 *
 * The catalog presents a category tab bar (Todo + one tab per store category + Descuentos) over a
 * single two-row, horizontally-scrolling product grid that shows the products of the selected tab.
 *
 * @property isLoading the catalog request is in flight (initial load).
 * @property isNetworkError the load failed and the connection-lost screen is shown.
 * @property tabs the category tab bar; empty until a successful, non-empty load.
 * @property selectedTabId the currently selected tab's id.
 * @property visibleProducts the products shown in the grid for the selected tab.
 * @property source where the catalog entry originated (drives mic-animation retention).
 * @property query the current bottom search-bar text for the search hand-off.
 * @property cartItemCount total items in the cart, shown on the Ir al carrito control's badge.
 * @property showCloseConfirmDialog whether the close-current-session confirmation dialog is visible.
 */
data class CatalogUiState(
    val isLoading: Boolean = true,
    val isNetworkError: Boolean = false,
    val tabs: ImmutableList<CatalogTab> = persistentListOf(),
    val selectedTabId: String = CatalogTab.ALL_ID,
    val visibleProducts: ImmutableList<Product> = persistentListOf(),
    val source: SearchSource = SearchSource.Text,
    val query: String = "",
    val cartItemCount: Int = 0,
    val showCloseConfirmDialog: Boolean = false,
) {
    /** The microphone animation is retained when the catalog was entered from a voice/chat context. */
    val showMicAnimation: Boolean get() = source == SearchSource.Voice

    /** True once a successful load returns no store categories — the whole-store empty state. */
    val isStoreEmpty: Boolean get() = !isLoading && !isNetworkError && tabs.isEmpty()

    /** True when the tab bar + product grid should be shown. */
    val showContent: Boolean get() = !isLoading && !isNetworkError && tabs.isNotEmpty()

    /** True when the Descuentos tab is selected and has no products (show the empty message). */
    val showDiscountsEmpty: Boolean
        get() = showContent && selectedTabId == CatalogTab.DISCOUNTS_ID && visibleProducts.isEmpty()
}
