package com.qhathia.mobile.discovery.feature.productsearch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.ui.component.DiscoverySearchBar
import com.qhathia.mobile.discovery.core.ui.component.HorizontalRowsGrid
import com.qhathia.mobile.discovery.core.ui.component.MicButton
import com.qhathia.mobile.discovery.core.ui.component.ProcessingMessage
import com.qhathia.mobile.discovery.core.ui.component.ProductCard
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.productsearch.R
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchPhase
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiEvent
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiState
import kotlinx.collections.immutable.toImmutableList
import com.qhathia.mobile.discovery.core.ui.R as UiR

/**
 * Stateless product-search UI — a pure function of [ProductSearchUiState], built from the Qhathia
 * design system and following the catalog-from-chat / unhappy Figma frames (the filters & suggestions
 * sidebar shown in Figma is intentionally out of scope). The body swaps by phase; the top bar is
 * always present and the bottom search bar + mic show on settled phases.
 */
@Composable
fun ProductSearchContent(
    uiState: ProductSearchUiState,
    onEvent: (ProductSearchUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                onBack = { onEvent(ProductSearchUiEvent.Back) },
                onNewSearch = { onEvent(ProductSearchUiEvent.NewSearch) },
                onCart = { /* cart navigation out of scope */ },
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (val phase = uiState.phase) {
                    ProductSearchPhase.VoiceListening,
                    ProductSearchPhase.VoiceTranscribing,
                    ProductSearchPhase.VoiceSlowResponse,
                    ProductSearchPhase.Searching,
                    ProductSearchPhase.SlowSearch,
                    ProductSearchPhase.SearchingAlternatives,
                    -> ProcessingBody(
                        uiState = uiState,
                        onEvent = onEvent,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    is ProductSearchPhase.Results -> ResultsBody(phase = phase, onEvent = onEvent)

                    is ProductSearchPhase.Unhappy -> UnhappyBody(
                        phase = phase,
                        onEvent = onEvent,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    ProductSearchPhase.NetworkError -> NetworkErrorBody(modifier = Modifier.align(Alignment.Center))
                }
            }

            if (!uiState.isProcessing) {
                BottomBar(query = uiState.query, onEvent = onEvent)
            }
        }

        if (uiState.showCloseConfirmDialog) {
            CloseConfirmDialog(
                onConfirm = { onEvent(ProductSearchUiEvent.ConfirmCloseSearch) },
                onDismiss = { onEvent(ProductSearchUiEvent.DismissDialog) },
            )
        }
    }
}

@Composable
private fun TopBar(
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
            text = stringResource(R.string.ps_back),
            contentDescription = stringResource(R.string.ps_back_cd),
            onClick = onBack,
            leadingIcon = true,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconTextButton(
            iconRes = UiR.drawable.ic_refresh,
            text = stringResource(R.string.ps_new_search),
            contentDescription = stringResource(R.string.ps_new_search_cd),
            onClick = onNewSearch,
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        CartButton(onClick = onCart)
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
            icon()
            label()
        } else {
            label()
            icon()
        }
    }
}

@Composable
private fun CartButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(DiscoveryColors.MicGradientStart, DiscoveryColors.MicGradientEnd),
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.ps_go_to_cart),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
        Icon(
            painter = painterResource(UiR.drawable.ic_cart),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ProcessingBody(
    uiState: ProductSearchUiState,
    onEvent: (ProductSearchUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // While listening the mic stops recording on tap; while transcribing it cancels the capture.
    val micOnClick: () -> Unit = when (uiState.phase) {
        ProductSearchPhase.VoiceListening -> { { onEvent(ProductSearchUiEvent.StopRecording) } }
        ProductSearchPhase.VoiceTranscribing,
        ProductSearchPhase.VoiceSlowResponse,
        -> { { onEvent(ProductSearchUiEvent.CancelVoice) } }
        else -> { {} }
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (uiState.showProcessingMicAnimation) {
            MicButton(
                isListening = true,
                contentDescription = stringResource(R.string.ps_mic_cd),
                onClick = micOnClick,
            )
        } else {
            BrandDot()
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
        uiState.phase.processingTextRes()?.let { res ->
            ProcessingMessage(baseText = stringResource(res))
        }
    }
}

@Composable
private fun ResultsBody(
    phase: ProductSearchPhase.Results,
    onEvent: (ProductSearchUiEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            BrandDot(size = 14.dp)
            Text(
                text = pluralStringResource(
                    R.plurals.ps_results_headline,
                    phase.products.size,
                    phase.products.size,
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        HorizontalRowsGrid(
            items = phase.products,
            rows = 2,
            key = { it.id },
            modifier = Modifier.fillMaxWidth(),
        ) { product ->
            ProductCard(
                name = product.name,
                priceText = stringResource(R.string.ps_price, product.price),
                imageUrl = product.imageUrl,
                cartContentDescription = stringResource(R.string.ps_cart_cd),
                onCartClick = { onEvent(ProductSearchUiEvent.CartTapped(product.id)) },
                onCardClick = { onEvent(ProductSearchUiEvent.ProductTapped(product.id)) },
            )
        }
    }
}

@Composable
private fun UnhappyBody(
    phase: ProductSearchPhase.Unhappy,
    onEvent: (ProductSearchUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandDot()
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.ps_unhappy_message),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 576.dp),
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            phase.alternatives.forEach { product ->
                ProductCard(
                    name = product.name,
                    priceText = stringResource(R.string.ps_price, product.price),
                    imageUrl = product.imageUrl,
                    cartContentDescription = stringResource(R.string.ps_cart_cd),
                    onCartClick = { onEvent(ProductSearchUiEvent.CartTapped(product.id)) },
                    onCardClick = { onEvent(ProductSearchUiEvent.ProductTapped(product.id)) },
                )
            }
        }
    }
}

@Composable
private fun NetworkErrorBody(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.ps_network_error_title),
            style = MaterialTheme.typography.headlineMedium,
            color = DiscoveryColors.Blue600,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.ps_network_error_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 480.dp),
        )
    }
}

@Composable
private fun BottomBar(
    query: String,
    onEvent: (ProductSearchUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl),
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
                text = stringResource(R.string.ps_prompt),
                style = MaterialTheme.typography.titleSmall,
                color = DiscoveryColors.Blue800,
            )
        }
        DiscoverySearchBar(
            query = query,
            onQueryChange = { onEvent(ProductSearchUiEvent.QueryChanged(it)) },
            onSearch = { onEvent(ProductSearchUiEvent.SubmitSearch) },
            placeholder = stringResource(R.string.ps_search_placeholder),
            searchIconContentDescription = stringResource(R.string.ps_search_icon_cd),
            modifier = Modifier.weight(1f),
        )
        MicButton(
            isListening = false,
            compact = true,
            contentDescription = stringResource(R.string.ps_mic_cd),
            onClick = { onEvent(ProductSearchUiEvent.ActivateMic) },
        )
    }
}

@Composable
private fun BrandDot(size: Dp = 16.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
    )
}

@Composable
private fun CloseConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ps_close_dialog_title)) },
        text = { Text(stringResource(R.string.ps_close_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.ps_close_dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ps_close_dialog_dismiss)) }
        },
    )
}

// --- Previews (landscape kiosk) ---

private fun sampleProducts(count: Int): List<Product> = (1..count).map {
    Product(
        id = "p$it",
        name = "Taladro Percutor Bosch GSB 13 RE Professional",
        description = "",
        price = 380.0,
        availability = "AVAILABLE",
        categoryName = "Taladros",
        imageUrl = "",
    )
}

@Preview(name = "Results — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun ResultsLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductSearchContent(
            uiState = ProductSearchUiState(
                source = SearchSource.Voice,
                phase = ProductSearchPhase.Results(sampleProducts(8).toImmutableList(), "Encontré 8 taladros para ti"),
                canGoBack = true,
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Results — dark", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun ResultsDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductSearchContent(
            uiState = ProductSearchUiState(
                source = SearchSource.Text,
                phase = ProductSearchPhase.Results(sampleProducts(6).toImmutableList(), "Encontré 6 taladros para ti"),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Searching (voice) — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun SearchingVoicePreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductSearchContent(
            uiState = ProductSearchUiState(source = SearchSource.Voice, phase = ProductSearchPhase.Searching),
            onEvent = {},
        )
    }
}

@Preview(name = "Slow search (text) — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun SlowSearchTextPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductSearchContent(
            uiState = ProductSearchUiState(source = SearchSource.Text, phase = ProductSearchPhase.SlowSearch),
            onEvent = {},
        )
    }
}

@Preview(name = "Searching alternatives — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun SearchingAlternativesPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductSearchContent(
            uiState = ProductSearchUiState(source = SearchSource.Voice, phase = ProductSearchPhase.SearchingAlternatives),
            onEvent = {},
        )
    }
}

@Preview(name = "Unhappy — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun UnhappyLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductSearchContent(
            uiState = ProductSearchUiState(
                source = SearchSource.Text,
                phase = ProductSearchPhase.Unhappy(sampleProducts(3).toImmutableList()),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Unhappy — dark", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun UnhappyDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductSearchContent(
            uiState = ProductSearchUiState(
                source = SearchSource.Text,
                phase = ProductSearchPhase.Unhappy(sampleProducts(3).toImmutableList()),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Network error — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun NetworkErrorPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductSearchContent(
            uiState = ProductSearchUiState(source = SearchSource.Text, phase = ProductSearchPhase.NetworkError),
            onEvent = {},
        )
    }
}

@Preview(name = "Close dialog — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun CloseDialogPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductSearchContent(
            uiState = ProductSearchUiState(
                source = SearchSource.Text,
                phase = ProductSearchPhase.Results(sampleProducts(4).toImmutableList(), "Encontré 4 taladros para ti"),
                showCloseConfirmDialog = true,
            ),
            onEvent = {},
        )
    }
}
