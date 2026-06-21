package com.qhathia.mobile.discovery.feature.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.domain.CatalogConstants
import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.ui.component.CartButton
import com.qhathia.mobile.discovery.core.ui.component.ConfirmationDialog
import com.qhathia.mobile.discovery.core.ui.component.DiscoverySearchBar
import com.qhathia.mobile.discovery.core.ui.component.HorizontalRowsGrid
import com.qhathia.mobile.discovery.core.ui.component.MicButton
import com.qhathia.mobile.discovery.core.ui.component.ProductCard
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.catalog.R
import com.qhathia.mobile.discovery.feature.catalog.model.CatalogTab
import com.qhathia.mobile.discovery.feature.catalog.model.CatalogUiEvent
import com.qhathia.mobile.discovery.feature.catalog.model.CatalogUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import com.qhathia.mobile.discovery.core.ui.R as UiR

/**
 * Stateless catalog UI — a pure function of [CatalogUiState], built from the Qhathia design system to
 * match the catalog Figma frame (node 758-25823): a toolbar, a centered category tab bar (Todo + one
 * tab per store category + Descuentos), a single two-row horizontally-scrolling product grid for the
 * selected tab, and a bottom chat-area with the prompt, search bar, and microphone.
 */
@Composable
fun CatalogContent(
    uiState: CatalogUiState,
    onEvent: (CatalogUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                cartItemCount = uiState.cartItemCount,
                onBack = { onEvent(CatalogUiEvent.Back) },
                onNewSearch = { onEvent(CatalogUiEvent.NewSearch) },
                onCart = { onEvent(CatalogUiEvent.OpenCart) },
            )

            if (uiState.showContent) {
                CategoryTabs(
                    tabs = uiState.tabs,
                    selectedTabId = uiState.selectedTabId,
                    onSelect = { onEvent(CatalogUiEvent.TabSelected(it)) },
                )
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    uiState.isLoading ->
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    uiState.isNetworkError ->
                        NetworkErrorBody(modifier = Modifier.align(Alignment.Center))

                    uiState.isStoreEmpty ->
                        StoreEmptyBody(modifier = Modifier.align(Alignment.Center))

                    uiState.showDiscountsEmpty ->
                        DiscountsEmptyBody(modifier = Modifier.align(Alignment.Center))

                    else -> HorizontalRowsGrid(
                        items = uiState.visibleProducts,
                        rows = CatalogConstants.MAX_CATEGORY_ROWS,
                        key = { it.id },
                        modifier = Modifier.align(Alignment.Center).fillMaxSize(),
                    ) { product ->
                        ProductCard(
                            name = product.name,
                            priceText = stringResource(UiR.string.discovery_price_format, product.price),
                            imageUrl = product.imageUrl,
                            cartContentDescription = stringResource(UiR.string.discovery_cd_add_to_cart),
                            onCartClick = { onEvent(CatalogUiEvent.CartTapped(product.id)) },
                            onCardClick = { onEvent(CatalogUiEvent.ProductTapped(product.id)) },
                        )
                    }
                }
            }

            if (!uiState.isLoading) {
                BottomBar(
                    query = uiState.query,
                    showMicAnimation = uiState.showMicAnimation,
                    onEvent = onEvent,
                )
            }
        }

        if (uiState.showCloseConfirmDialog) {
            CloseConfirmDialog(
                onConfirm = { onEvent(CatalogUiEvent.ConfirmCloseSearch) },
                onDismiss = { onEvent(CatalogUiEvent.DismissDialog) },
            )
        }
    }
}

@Composable
private fun TopBar(
    cartItemCount: Int,
    onBack: () -> Unit,
    onNewSearch: () -> Unit,
    onCart: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTextButton(
            iconRes = UiR.drawable.ic_arrow_back,
            text = stringResource(UiR.string.discovery_action_back),
            contentDescription = stringResource(UiR.string.discovery_cd_back),
            onClick = onBack,
            leadingIcon = true,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconTextButton(
            iconRes = UiR.drawable.ic_refresh,
            text = stringResource(UiR.string.discovery_action_new_search),
            contentDescription = stringResource(UiR.string.discovery_cd_new_search),
            onClick = onNewSearch,
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        CartButton(
            itemCount = cartItemCount,
            label = stringResource(UiR.string.discovery_action_go_to_cart),
            contentDescription = stringResource(UiR.string.discovery_action_go_to_cart),
            onClick = onCart,
        )
    }
}

@Composable
private fun IconTextButton(
    iconRes: Int,
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Boolean = false,
) {
    val icon = @Composable {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = DiscoveryColors.Blue800,
            modifier = Modifier.size(20.dp),
        )
    }
    val label = @Composable {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = DiscoveryColors.Blue800)
    }
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (leadingIcon) {
            icon(); label()
        } else {
            label(); icon()
        }
    }
}

/**
 * The centered, horizontally-scrollable category tab bar. A neutral rounded track holds pill tabs;
 * the selected tab is a white pill, the others are plain text. The Descuentos tab carries a discount
 * icon.
 */
@Composable
private fun CategoryTabs(
    tabs: ImmutableList<CatalogTab>,
    selectedTabId: String,
    onSelect: (String) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(DiscoveryColors.CatalogTabTrack)
                .horizontalScroll(rememberScrollState())
                .padding(Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                CategoryTab(
                    label = tabLabel(tab),
                    selected = tab.id == selectedTabId,
                    hasIcon = tab.kind == CatalogTab.Kind.Discounts,
                    onClick = { onSelect(tab.id) },
                )
            }
        }
    }
}

@Composable
private fun tabLabel(tab: CatalogTab): String = when (tab.kind) {
    CatalogTab.Kind.All -> stringResource(R.string.catalog_tab_all)
    CatalogTab.Kind.Discounts -> stringResource(R.string.catalog_discounts_heading)
    CatalogTab.Kind.Category -> tab.label
}

@Composable
private fun CategoryTab(
    label: String,
    selected: Boolean,
    hasIcon: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val contentColor = if (selected) DiscoveryColors.CatalogText else DiscoveryColors.TextMuted
    Row(
        modifier = Modifier
            .then(if (selected) Modifier.shadow(elevation = 3.dp, shape = shape, clip = false) else Modifier)
            .clip(shape)
            .then(if (selected) Modifier.background(Color.White) else Modifier)
            .clickable(onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
        )
        if (hasIcon) {
            Icon(
                painter = painterResource(UiR.drawable.ic_discount),
                contentDescription = null, // the "Descuentos" label is the accessible name
                tint = contentColor,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun DiscountsEmptyBody(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(horizontal = Spacing.lg), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.catalog_discounts_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NetworkErrorBody(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(UiR.string.discovery_error_network_title),
            style = MaterialTheme.typography.headlineMedium,
            color = DiscoveryColors.Blue600,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(Spacing.sm))
        Text(
            text = stringResource(UiR.string.discovery_error_network_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 480.dp),
        )
    }
}

@Composable
private fun StoreEmptyBody(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(horizontal = Spacing.lg), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.catalog_store_empty),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 480.dp),
        )
    }
}

@Composable
private fun BottomBar(
    query: String,
    showMicAnimation: Boolean,
    onEvent: (CatalogUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl,),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                painter = painterResource(UiR.drawable.ic_lightbulb),
                contentDescription = null,
                tint = DiscoveryColors.Blue600,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = stringResource(UiR.string.discovery_search_prompt),
                style = MaterialTheme.typography.titleSmall,
                color = DiscoveryColors.Blue800,
            )
        }
        DiscoverySearchBar(
            query = query,
            onQueryChange = { onEvent(CatalogUiEvent.QueryChanged(it)) },
            onSearch = { onEvent(CatalogUiEvent.SubmitSearch) },
            placeholder = stringResource(UiR.string.discovery_search_placeholder),
            searchIconContentDescription = stringResource(UiR.string.discovery_cd_search),
            modifier = Modifier.weight(1f),
        )
        MicButton(
            isListening = showMicAnimation,
            compact = true,
            contentDescription = stringResource(UiR.string.discovery_cd_voice_search),
            onClick = { onEvent(CatalogUiEvent.ActivateMic) },
        )
    }
}

@Composable
private fun CloseConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(R.string.catalog_close_dialog_title),
        message = stringResource(R.string.catalog_close_dialog_message),
        confirmLabel = stringResource(UiR.string.discovery_dialog_confirm_close),
        dismissLabel = stringResource(UiR.string.discovery_dialog_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

// --- Previews (landscape kiosk) ---

private fun sampleProduct(id: String, name: String, discount: Boolean = false): Product = Product(
    id = id,
    name = name,
    description = "",
    price = 380.0,
    availability = "AVAILABLE",
    categoryName = "Iluminación",
    imageUrl = "",
    allowDiscountInquiry = discount,
)

private fun sampleTabs(): ImmutableList<CatalogTab> = persistentListOf(
    CatalogTab(id = CatalogTab.ALL_ID, kind = CatalogTab.Kind.All),
    CatalogTab(id = CatalogTab.categoryId("c1"), kind = CatalogTab.Kind.Category, label = "Electrónica"),
    CatalogTab(id = CatalogTab.categoryId("c2"), kind = CatalogTab.Kind.Category, label = "Carpintería"),
    CatalogTab(id = CatalogTab.categoryId("c3"), kind = CatalogTab.Kind.Category, label = "Iluminación"),
    CatalogTab(id = CatalogTab.DISCOUNTS_ID, kind = CatalogTab.Kind.Discounts),
)

private fun sampleProducts(count: Int): ImmutableList<Product> =
    (1..count).map { sampleProduct("p$it", "Taladro Percutor Bosch GSB 13 RE Professional", discount = it <= 2) }
        .toImmutableList()

@Preview(name = "Catalog (Todo) — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun CatalogLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        CatalogContent(
            uiState = CatalogUiState(
                isLoading = false,
                tabs = sampleTabs(),
                selectedTabId = CatalogTab.ALL_ID,
                visibleProducts = sampleProducts(12),
                cartItemCount = 2,
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Catalog (category) — dark", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun CatalogDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        CatalogContent(
            uiState = CatalogUiState(
                isLoading = false,
                tabs = sampleTabs(),
                selectedTabId = CatalogTab.categoryId("c3"),
                visibleProducts = sampleProducts(6),
                source = SearchSource.Voice,
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Catalog discounts empty — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun CatalogDiscountsEmptyPreview() {
    DiscoveryTheme(darkTheme = false) {
        CatalogContent(
            uiState = CatalogUiState(
                isLoading = false,
                tabs = sampleTabs(),
                selectedTabId = CatalogTab.DISCOUNTS_ID,
                visibleProducts = persistentListOf(),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Catalog store empty — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun CatalogStoreEmptyPreview() {
    DiscoveryTheme(darkTheme = false) {
        CatalogContent(
            uiState = CatalogUiState(isLoading = false, tabs = persistentListOf()),
            onEvent = {},
        )
    }
}

@Preview(name = "Catalog network error — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun CatalogNetworkErrorPreview() {
    DiscoveryTheme(darkTheme = false) {
        CatalogContent(
            uiState = CatalogUiState(isLoading = false, isNetworkError = true),
            onEvent = {},
        )
    }
}

@Preview(name = "Catalog close dialog — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun CatalogCloseDialogPreview() {
    DiscoveryTheme(darkTheme = false) {
        CatalogContent(
            uiState = CatalogUiState(
                isLoading = false,
                tabs = sampleTabs(),
                visibleProducts = sampleProducts(8),
                showCloseConfirmDialog = true,
            ),
            onEvent = {},
        )
    }
}
