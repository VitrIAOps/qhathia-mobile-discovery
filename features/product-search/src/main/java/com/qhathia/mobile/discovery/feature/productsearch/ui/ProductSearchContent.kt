package com.qhathia.mobile.discovery.feature.productsearch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qhathia.mobile.discovery.core.domain.usecase.isOrderable
import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.SearchFilter
import com.qhathia.mobile.discovery.core.domain.model.SearchFilterOption
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.ui.component.CartButton
import com.qhathia.mobile.discovery.core.ui.component.closeDialogMessage
import com.qhathia.mobile.discovery.core.ui.component.ConfirmationDialog
import com.qhathia.mobile.discovery.core.ui.component.DiscoveryDivider
import com.qhathia.mobile.discovery.core.ui.component.DiscoverySearchBar
import com.qhathia.mobile.discovery.core.ui.component.HorizontalRowsGrid
import com.qhathia.mobile.discovery.core.ui.component.MicButton
import com.qhathia.mobile.discovery.core.ui.component.ListeningMessage
import com.qhathia.mobile.discovery.core.ui.component.PrimaryActionButton
import com.qhathia.mobile.discovery.core.ui.component.ProductCard
import com.qhathia.mobile.discovery.core.ui.component.labeled
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.productsearch.R
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchPhase
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiEvent
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import com.qhathia.mobile.discovery.core.ui.R as UiR

/**
 * Stateless product-search UI — a pure function of [ProductSearchUiState], built from the Qhathia
 * design system and following the catalog-from-chat / unhappy Figma frames. On the results phase a
 * filters & suggestions panel (Figma 722-13413 / 722-13564) lets the consumer refine the search; it
 * is hidden when the response carried neither. The body swaps by phase; the top bar is always present
 * and the bottom search bar + mic show on settled phases.
 */
@Composable
fun ProductSearchContent(
    uiState: ProductSearchUiState,
    onEvent: (ProductSearchUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        val phase = uiState.phase
        // The refine sidebar spans the full screen height (Figma 722-13413), wrapping the entire
        // results column — the top bar and bottom search bar sit to its right, not above/below it.
        // It shows only on a results phase whose response carried filters or suggestions.
        val refinePhase = (phase as? ProductSearchPhase.Results)
            ?.takeIf { it.filters.isNotEmpty() || it.suggestions.isNotEmpty() }

        Row(modifier = Modifier.fillMaxSize()) {
            if (refinePhase != null) {
                FiltersSuggestionsPanel(
                    filters = refinePhase.filters,
                    suggestions = refinePhase.suggestions,
                    selectedFilters = uiState.selectedFilters,
                    selectedSuggestion = uiState.selectedSuggestion,
                    onEvent = onEvent,
                )
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                TopBar(
                    cartItemCount = uiState.cartItemCount,
                    onBack = { onEvent(ProductSearchUiEvent.Back) },
                    onGoToCatalog = { onEvent(ProductSearchUiEvent.GoToCatalog) },
                    onNewSearch = { onEvent(ProductSearchUiEvent.NewSearch) },
                    onCart = { onEvent(ProductSearchUiEvent.OpenCart) },
                )

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when (phase) {
                        is ProductSearchPhase.VoiceListening,
                        is ProductSearchPhase.VoiceTranscribing,
                        ProductSearchPhase.VoiceSlowResponse,
                        ProductSearchPhase.Searching,
                        ProductSearchPhase.SlowSearch,
                        -> ProcessingBody(
                            uiState = uiState,
                            onEvent = onEvent,
                            modifier = Modifier.align(Alignment.Center),
                        )

                        is ProductSearchPhase.Results -> ResultsBody(
                            phase = phase,
                            onEvent = onEvent,
                        )

                        is ProductSearchPhase.Unhappy -> UnhappyBody(
                            phase = phase,
                            onEvent = onEvent,
                            modifier = Modifier.align(Alignment.Center),
                        )

                        ProductSearchPhase.NetworkError -> NetworkErrorBody(
                            onRetry = { onEvent(ProductSearchUiEvent.Retry) },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                if (!uiState.isProcessing) {
                    BottomBar(query = uiState.query, onEvent = onEvent)
                }
            }
        }

        if (uiState.showCloseConfirmDialog) {
            CloseConfirmDialog(
                cartItemCount = uiState.cartItemCount,
                onConfirm = { onEvent(ProductSearchUiEvent.ConfirmCloseSearch) },
                onDismiss = { onEvent(ProductSearchUiEvent.DismissDialog) },
            )
        }
    }
}

@Composable
private fun TopBar(
    cartItemCount: Int,
    onBack: () -> Unit,
    onGoToCatalog: () -> Unit,
    onNewSearch: () -> Unit,
    onCart: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.sm),
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
                iconRes = UiR.drawable.ic_catalog,
                text = stringResource(UiR.string.discovery_action_go_to_catalog),
                contentDescription = stringResource(UiR.string.discovery_cd_go_to_catalog),
                onClick = onGoToCatalog,
            )
            Spacer(modifier = Modifier.width(Spacing.md))
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
        // Hairline separating the toolbar from the results (Figma 722-13419 border-b).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .height(0.dp)
                .background(DiscoveryColors.Blue200),
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
            contentDescription = null, // the label is on the button itself
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
            .labeled(contentDescription)
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
private fun ProcessingBody(
    uiState: ProductSearchUiState,
    onEvent: (ProductSearchUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // While listening the mic stops recording on tap; while transcribing it cancels the capture.
    val micOnClick: () -> Unit = when (uiState.phase) {
        is ProductSearchPhase.VoiceListening -> { { onEvent(ProductSearchUiEvent.StopRecording) } }
        is ProductSearchPhase.VoiceTranscribing,
        ProductSearchPhase.VoiceSlowResponse,
        -> { { onEvent(ProductSearchUiEvent.CancelVoice) } }
        else -> { {} }
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (uiState.showProcessingMicAnimation) {
            MicButton(
                isListening = true,
                contentDescription = stringResource(UiR.string.discovery_cd_voice_search),
                onClick = micOnClick,
            )
        } else {
            BrandDot()
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
        uiState.phase.processingTextRes()?.let { res ->
            // The heard text replaces "Escuchando…" / "Organizando ideas…" as soon as words are recognized.
            val liveTranscript = when (val phase = uiState.phase) {
                is ProductSearchPhase.VoiceListening -> phase.liveTranscript
                is ProductSearchPhase.VoiceTranscribing -> phase.liveTranscript
                else -> ""
            }
            ListeningMessage(transcript = liveTranscript, listeningText = stringResource(res))
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
            modifier = Modifier
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            BrandDot(size = 14.dp)
            Text(
                text = when {
                    phase.noFilterMatch -> stringResource(R.string.ps_filters_no_match)
                    phase.missingCategory != null -> stringResource(R.string.ps_alternatives_only, phase.missingCategory)
                    else -> pluralStringResource(R.plurals.ps_results_headline, phase.products.size, phase.products.size)
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (phase.noFilterMatch) return@Column
        HorizontalRowsGrid(
            items = phase.products,
            rows = 2,
            key = { it.id },
            modifier = Modifier.fillMaxWidth(),
        ) { product ->
            ProductCard(
                name = product.name,
                priceText = stringResource(
                    UiR.string.discovery_price_format,
                    product.discountedPrice ?: product.price,
                ),
                originalPriceText = product.discountedPrice?.let {
                    stringResource(UiR.string.discovery_price_format, product.price)
                },
                imageUrl = product.imageUrl,
                cartContentDescription = stringResource(UiR.string.discovery_cd_add_to_cart),
                onCartClick = { onEvent(ProductSearchUiEvent.CartTapped(product.id)) },
                onCardClick = { onEvent(ProductSearchUiEvent.ProductTapped(product.id)) },
                cartEnabled = product.isOrderable,
            )
        }
    }
}

/**
 * The left "Afina tu búsqueda" panel (Figma 722-13413): a fixed header, one scrollable section per
 * filter criterion (each option a toggleable chip), and a highlighted Sugerencias section pinned to
 * the bottom edge. Selected chips render filled per Figma. The panel carries the design's 40dp
 * vertical inset and a soft right-edge shadow.
 */
@Composable
private fun FiltersSuggestionsPanel(
    filters: ImmutableList<SearchFilter>,
    suggestions: ImmutableList<String>,
    selectedFilters: ImmutableSet<SearchFilterOption>,
    selectedSuggestion: String?,
    onEvent: (ProductSearchUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color.White)
            .padding(Spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    painter = painterResource(UiR.drawable.ic_filter),
                    contentDescription = stringResource(R.string.ps_refine_icon_cd),
                    tint = DiscoveryColors.CatalogText,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.ps_refine_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = DiscoveryColors.CatalogText,
                )
            }
            Text(
                text = stringResource(R.string.ps_refine_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = DiscoveryColors.TextMuted,
            )
            DiscoveryDivider()
        }

        // Criterion sections take the remaining height (scrolling on overflow) so the highlighted
        // Sugerencias card settles against the bottom edge, matching the Figma justify-between layout.
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = Spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            filters.forEach { filter ->
                CriterionSection(
                    criterion = filter.criterion,
                    options = filter.options,
                    selectedFilters = selectedFilters,
                    onEvent = onEvent,
                )
            }
        }

        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.md))
            SuggestionsSection(
                suggestions = suggestions,
                selectedSuggestion = selectedSuggestion,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun CriterionSection(
    criterion: String,
    options: List<String>,
    selectedFilters: ImmutableSet<SearchFilterOption>,
    onEvent: (ProductSearchUiEvent) -> Unit,
) {
    val hasSelection = selectedFilters.any { it.criterion == criterion }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    painter = painterResource(UiR.drawable.ic_tag),
                    contentDescription = null,
                    tint = DiscoveryColors.CatalogText,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = criterion,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, letterSpacing = 0.1.sp),
                    color = DiscoveryColors.CatalogText,
                )
            }
            // "Limpiar" only appears once this criterion has a selection to clear.
            if (hasSelection) {
                val clearCd = stringResource(R.string.ps_filter_clear_cd, criterion)
                Text(
                    text = stringResource(R.string.ps_filter_clear),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    color = DiscoveryColors.LinkBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onEvent(ProductSearchUiEvent.FilterCriterionCleared(criterion)) }
                        .semantics { contentDescription = clearCd }
                        .padding(Spacing.xs),
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            options.forEach { option ->
                RefineChip(
                    label = option,
                    selected = SearchFilterOption(criterion, option) in selectedFilters,
                    contentDescription = stringResource(R.string.ps_filter_option_cd, criterion, option),
                    onClick = { onEvent(ProductSearchUiEvent.FilterToggled(criterion, option)) },
                )
            }
        }
    }
}

@Composable
private fun SuggestionsSection(
    suggestions: ImmutableList<String>,
    selectedSuggestion: String?,
    onEvent: (ProductSearchUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, DiscoveryColors.AccentOrange, RoundedCornerShape(16.dp))
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                painter = painterResource(UiR.drawable.ic_sparkle),
                contentDescription = stringResource(R.string.ps_suggestions_icon_cd),
                tint = DiscoveryColors.AccentOrange,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.ps_suggestions_title),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                color = DiscoveryColors.CatalogText,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            suggestions.forEach { phrase ->
                RefineChip(
                    label = phrase,
                    selected = phrase == selectedSuggestion,
                    contentDescription = stringResource(R.string.ps_suggestion_cd, phrase),
                    onClick = { onEvent(ProductSearchUiEvent.SuggestionSelected(phrase)) },
                )
            }
        }
    }
}

/** A toggleable refine chip: white with a blue-100 border by default, filled blue when [selected]. */
@Composable
private fun RefineChip(
    label: String,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    // Selected chips use a fixed dark-blue fill, so pair it with a fixed light label for contrast in
    // both themes; default chips follow the theme's surface/onSurface roles.
    val background = if (selected) DiscoveryColors.Blue500 else MaterialTheme.colorScheme.surface
    val labelColor = if (selected) DiscoveryColors.Neutral50 else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .clip(shape)
            .background(background)
            .then(if (selected) Modifier else Modifier.border(1.dp, DiscoveryColors.Blue100, shape))
            .toggleable(value = selected, onValueChange = { onClick() }, role = Role.Checkbox)
            .labeled(contentDescription, role = null)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor,
        )
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
        // Title: the apology, whose invite line is personalized from the category the backend drew the
        // suggestions from ("Relacionado a 'lámpara' te podría interesar…") or the generic invite.
        val invite = phase.suggestedFromCategory
            ?.let { stringResource(R.string.ps_unhappy_invite_category, it) }
            ?: stringResource(R.string.ps_unhappy_invite)
        Text(
            text = "${stringResource(R.string.ps_unhappy_message)}\n$invite",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 576.dp),
        )
        if (phase.alternatives.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                phase.alternatives.forEach { product ->
                    ProductCard(
                        name = product.name,
                        priceText = stringResource(
                            UiR.string.discovery_price_format,
                            product.discountedPrice ?: product.price,
                        ),
                        originalPriceText = product.discountedPrice?.let {
                            stringResource(UiR.string.discovery_price_format, product.price)
                        },
                        imageUrl = product.imageUrl,
                        cartContentDescription = stringResource(UiR.string.discovery_cd_add_to_cart),
                        onCartClick = { onEvent(ProductSearchUiEvent.CartTapped(product.id)) },
                        onCardClick = { onEvent(ProductSearchUiEvent.ProductTapped(product.id)) },
                        cartEnabled = product.isOrderable,
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkErrorBody(onRetry: () -> Unit, modifier: Modifier = Modifier) {
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
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = stringResource(UiR.string.discovery_error_network_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 480.dp),
        )
        // F-38: the same retry action as the Catalog and Product-detail error bodies.
        Spacer(modifier = Modifier.size(Spacing.md))
        PrimaryActionButton(
            text = stringResource(UiR.string.discovery_action_retry),
            onClick = onRetry,
            fillMaxWidth = false,
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
            .padding(horizontal = Spacing.xl)
            .padding(bottom = Spacing.sm),
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
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                color = DiscoveryColors.Blue800,
            )
        }
        DiscoverySearchBar(
            query = query,
            onQueryChange = { onEvent(ProductSearchUiEvent.QueryChanged(it)) },
            onSearch = { onEvent(ProductSearchUiEvent.SubmitSearch) },
            placeholder = stringResource(UiR.string.discovery_search_placeholder),
            searchIconContentDescription = stringResource(UiR.string.discovery_cd_search),
            modifier = Modifier.weight(1f),
        )
        MicButton(
            isListening = false,
            compact = true,
            contentDescription = stringResource(UiR.string.discovery_cd_voice_search),
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
    cartItemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(R.string.ps_close_dialog_title),
        message = closeDialogMessage(cartItemCount, R.string.ps_close_dialog_message),
        confirmLabel = stringResource(UiR.string.discovery_dialog_confirm_close),
        dismissLabel = stringResource(UiR.string.discovery_dialog_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
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
        discountedPrice = if (it <= 2) 320.0 else null,
    )
}

private fun sampleFilters(): List<SearchFilter> = listOf(
    SearchFilter(criterion = "Marca", options = listOf("Bosch", "Makita", "Dewalt")),
    SearchFilter(criterion = "Voltaje", options = listOf("12V", "18V", "20V")),
)

private fun sampleSuggestions(): List<String> = listOf(
    "Para uso en casa",
    "Para uso profesional",
    "Percutor",
    "Kit completo",
)

private fun resultsWithRefine(
    count: Int = 8,
    selectedFilters: Set<SearchFilterOption> = emptySet(),
    selectedSuggestion: String? = null,
): ProductSearchUiState = ProductSearchUiState(
    source = SearchSource.Voice,
    phase = ProductSearchPhase.Results(
        products = sampleProducts(count).toImmutableList(),
        displayText = "Encontré $count taladros para ti",
        filters = sampleFilters().toImmutableList(),
        suggestions = sampleSuggestions().toImmutableList(),
    ),
    canGoBack = true,
    selectedFilters = selectedFilters.toImmutableSet(),
    selectedSuggestion = selectedSuggestion,
)

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

@Preview(name = "Results + refine — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun ResultsRefineLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductSearchContent(uiState = resultsWithRefine(), onEvent = {})
    }
}

@Preview(name = "Results + refine — dark", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun ResultsRefineDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductSearchContent(uiState = resultsWithRefine(), onEvent = {})
    }
}

@Preview(name = "Results + refine selected — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun ResultsRefineSelectedLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductSearchContent(
            uiState = resultsWithRefine(
                count = 3,
                selectedFilters = setOf(SearchFilterOption("Marca", "Bosch"), SearchFilterOption("Voltaje", "12V")),
                selectedSuggestion = "Para uso en casa",
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Results + refine selected — dark", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun ResultsRefineSelectedDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductSearchContent(
            uiState = resultsWithRefine(
                count = 3,
                selectedFilters = setOf(SearchFilterOption("Marca", "Bosch"), SearchFilterOption("Voltaje", "12V")),
                selectedSuggestion = "Para uso en casa",
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Results — no filter match — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun ResultsNoFilterMatchLightPreview() {
    DiscoveryTheme(darkTheme = false) { ProductSearchContent(uiState = noFilterMatchState(), onEvent = {}) }
}

@Preview(name = "Results — no filter match — dark", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun ResultsNoFilterMatchDarkPreview() {
    DiscoveryTheme(darkTheme = true) { ProductSearchContent(uiState = noFilterMatchState(), onEvent = {}) }
}

private fun noFilterMatchState(): ProductSearchUiState = ProductSearchUiState(
    source = SearchSource.Voice,
    phase = ProductSearchPhase.Results(
        products = kotlinx.collections.immutable.persistentListOf(),
        displayText = "",
        filters = sampleFilters().toImmutableList(),
        suggestions = sampleSuggestions().toImmutableList(),
        noFilterMatch = true,
    ),
    canGoBack = true,
    selectedFilters = setOf(SearchFilterOption("Marca", "Bosch"), SearchFilterOption("Voltaje", "12V")).toImmutableSet(),
)

@Preview(name = "Results — alternatives only — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun ResultsAlternativesOnlyLightPreview() {
    DiscoveryTheme(darkTheme = false) { ProductSearchContent(uiState = alternativesOnlyState(), onEvent = {}) }
}

@Preview(name = "Results — alternatives only — dark", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun ResultsAlternativesOnlyDarkPreview() {
    DiscoveryTheme(darkTheme = true) { ProductSearchContent(uiState = alternativesOnlyState(), onEvent = {}) }
}

private fun alternativesOnlyState(): ProductSearchUiState = ProductSearchUiState(
    source = SearchSource.Text,
    phase = ProductSearchPhase.Results(
        products = sampleProducts(3).toImmutableList(),
        displayText = "",
        missingCategory = "ventilador",
    ),
)

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

@Preview(name = "Unhappy + category — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun UnhappyLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductSearchContent(
            uiState = ProductSearchUiState(
                source = SearchSource.Text,
                phase = ProductSearchPhase.Unhappy(
                    alternatives = sampleProducts(3).toImmutableList(),
                    suggestedFromCategory = "lámpara",
                ),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Unhappy + category — dark", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun UnhappyDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductSearchContent(
            uiState = ProductSearchUiState(
                source = SearchSource.Text,
                phase = ProductSearchPhase.Unhappy(
                    alternatives = sampleProducts(3).toImmutableList(),
                    suggestedFromCategory = "lámpara",
                ),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Unhappy — apology only (API error)", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun UnhappyApologyOnlyPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductSearchContent(
            uiState = ProductSearchUiState(
                source = SearchSource.Text,
                phase = ProductSearchPhase.Unhappy(alternatives = persistentListOf()),
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
            uiState = ProductSearchUiState(source = SearchSource.Text, phase = ProductSearchPhase.NetworkError, query = "martillo"),
            onEvent = {},
        )
    }
}

@Preview(name = "Network error — dark", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun NetworkErrorDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductSearchContent(
            uiState = ProductSearchUiState(source = SearchSource.Text, phase = ProductSearchPhase.NetworkError, query = "martillo"),
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

@Preview(name = "Close dialog, 2 in cart — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun CloseDialogWithCartLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductSearchContent(
            uiState = ProductSearchUiState(
                source = SearchSource.Text,
                phase = ProductSearchPhase.Results(sampleProducts(4).toImmutableList(), "Encontré 4 taladros para ti"),
                cartItemCount = 2,
                showCloseConfirmDialog = true,
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Close dialog, 2 in cart — dark", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun CloseDialogWithCartDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductSearchContent(
            uiState = ProductSearchUiState(
                source = SearchSource.Text,
                phase = ProductSearchPhase.Results(sampleProducts(4).toImmutableList(), "Encontré 4 taladros para ti"),
                cartItemCount = 2,
                showCloseConfirmDialog = true,
            ),
            onEvent = {},
        )
    }
}
