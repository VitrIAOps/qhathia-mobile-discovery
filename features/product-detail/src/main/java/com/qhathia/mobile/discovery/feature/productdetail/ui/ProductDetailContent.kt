package com.qhathia.mobile.discovery.feature.productdetail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductDetail
import com.qhathia.mobile.discovery.core.domain.model.ProductImage
import com.qhathia.mobile.discovery.core.ui.component.DiscoverySearchBar
import com.qhathia.mobile.discovery.core.ui.component.MicButton
import com.qhathia.mobile.discovery.core.ui.component.ProductImageGallery
import com.qhathia.mobile.discovery.core.ui.component.ProductRowCard
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.productdetail.R
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiEvent
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import com.qhathia.mobile.discovery.core.ui.R as UiR

/**
 * Stateless product-detail UI — a pure function of [ProductDetailUiState], following the detail Figma
 * (node 644-26093). The top bar and the bottom chat-area (prompt + search bar + microphone) are always
 * present; the body swaps by load status (loading / loaded / error).
 */
@Composable
fun ProductDetailContent(
    uiState: ProductDetailUiState,
    onEvent: (ProductDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                onBack = { onEvent(ProductDetailUiEvent.Back) },
                onNewSearch = { onEvent(ProductDetailUiEvent.NewSearch) },
                onCart = { /* cart navigation out of scope */ },
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (val status = uiState.status) {
                    ProductDetailUiState.Status.Loading ->
                        LoadingBody(modifier = Modifier.align(Alignment.Center))

                    is ProductDetailUiState.Status.Loaded ->
                        LoadedBody(detail = status.detail, related = status.relatedProducts, onEvent = onEvent)

                    is ProductDetailUiState.Status.Error ->
                        ErrorBody(kind = status.kind, onEvent = onEvent, modifier = Modifier.align(Alignment.Center))
                }
            }

            // Business rule: keep the bottom chat-area (prompt + search bar + microphone) on the detail screen.
            BottomBar(query = uiState.query, onEvent = onEvent)
        }

        if (uiState.showCloseConfirmDialog) {
            CloseConfirmDialog(
                onConfirm = { onEvent(ProductDetailUiEvent.ConfirmCloseSearch) },
                onDismiss = { onEvent(ProductDetailUiEvent.DismissDialog) },
            )
        }
    }
}

// --- Top bar (mirrors the product-search chrome; re-created here as features can't depend on each other) ---

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
            text = stringResource(R.string.pd_back),
            contentDescription = stringResource(R.string.pd_back_cd),
            onClick = onBack,
            leadingIcon = true,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconTextButton(
            iconRes = UiR.drawable.ic_refresh,
            text = stringResource(R.string.pd_new_search),
            contentDescription = stringResource(R.string.pd_new_search_cd),
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
            icon(); label()
        } else {
            label(); icon()
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
            text = stringResource(R.string.pd_go_to_cart),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
        Icon(
            painter = painterResource(UiR.drawable.ic_cart),
            contentDescription = stringResource(R.string.pd_cart_cd),
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

// --- Bodies ---

@Composable
private fun LoadingBody(modifier: Modifier = Modifier) {
    val loadingCd = stringResource(R.string.pd_loading_cd)
    CircularProgressIndicator(
        modifier = modifier.semantics { contentDescription = loadingCd },
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun LoadedBody(
    detail: ProductDetail,
    related: ImmutableList<Product>,
    onEvent: (ProductDetailUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        HeadingRow(text = stringResource(R.string.pd_detail_headline))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            ProductImageGallery(
                imageUrls = detail.images.map { it.url },
                mainContentDescription = stringResource(R.string.pd_product_image_cd),
            )
            ProductInfo(detail = detail, onEvent = onEvent, modifier = Modifier.weight(1f))
        }
        RelatedProductsSection(related = related, onEvent = onEvent)
        Spacer(modifier = Modifier.size(Spacing.sm))
    }
}

@Composable
private fun HeadingRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        BrandDot(size = 14.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ProductInfo(
    detail: ProductDetail,
    onEvent: (ProductDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            text = detail.name,
            style = MaterialTheme.typography.titleSmall,
            color = DiscoveryColors.CatalogText,
        )
        StockRow(availability = detail.availability)
        Text(
            text = stringResource(R.string.pd_price, detail.price),
            style = MaterialTheme.typography.headlineMedium,
            color = DiscoveryColors.CatalogText,
        )
        Box(modifier = Modifier.fillMaxWidth().size(1.dp).background(DiscoveryColors.Divider))
        Text(
            text = detail.description,
            style = MaterialTheme.typography.bodySmall,
            color = DiscoveryColors.TextMuted,
        )
        Spacer(modifier = Modifier.size(Spacing.xs))
        ActionRow(onEvent = onEvent)
    }
}

@Composable
private fun StockRow(availability: String) {
    val (dotColor, label) = availabilityDisplay(availability) ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = dotColor)
    }
}

@Composable
private fun ActionRow(onEvent: (ProductDetailUiEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuantityStepper()
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ActionButton(
                text = stringResource(R.string.pd_add_to_cart),
                iconRes = UiR.drawable.ic_cart,
                container = DiscoveryColors.Blue700,
                onClick = { onEvent(ProductDetailUiEvent.AddToCart) },
            )
            ActionButton(
                text = stringResource(R.string.pd_share_whatsapp),
                iconRes = R.drawable.ic_whatsapp,
                container = DiscoveryColors.Green500,
                onClick = { onEvent(ProductDetailUiEvent.ShareWhatsApp) },
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    iconRes: Int,
    container: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = Color.White),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.width(Spacing.sm))
        Icon(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(16.dp))
    }
}

/**
 * Render-only quantity stepper (the cart action is out of scope). The count is purely local UI state.
 */
@Composable
private fun QuantityStepper() {
    var quantity by remember { mutableIntStateOf(1) }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, DiscoveryColors.SearchBorder, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            symbol = "−", // minus sign
            contentDescription = stringResource(R.string.pd_qty_decrease_cd),
            onClick = { if (quantity > 1) quantity-- },
        )
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(min = 48.dp),
            textAlign = TextAlign.Center,
        )
        StepperButton(
            symbol = "+",
            contentDescription = stringResource(R.string.pd_qty_increase_cd),
            onClick = { quantity++ },
        )
    }
}

@Composable
private fun StepperButton(
    symbol: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RelatedProductsSection(
    related: ImmutableList<Product>,
    onEvent: (ProductDetailUiEvent) -> Unit,
) {
    // PLACEHOLDER: related products come from a mock until the related-products API lands.
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        HeadingRow(text = stringResource(R.string.pd_related_headline))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            related.forEach { product ->
                ProductRowCard(
                    name = product.name,
                    categoryName = product.categoryName,
                    priceText = stringResource(R.string.pd_price, product.price),
                    imageUrl = product.imageUrl,
                    onClick = { onEvent(ProductDetailUiEvent.RelatedProductTapped(product.id)) },
                )
            }
        }
    }
}

@Composable
private fun ErrorBody(
    kind: ProductDetailUiState.ErrorKind,
    onEvent: (ProductDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (titleRes, messageRes) = when (kind) {
        ProductDetailUiState.ErrorKind.NotFound -> R.string.pd_not_found_title to R.string.pd_not_found_message
        ProductDetailUiState.ErrorKind.Network -> R.string.pd_network_error_title to R.string.pd_network_error_message
        ProductDetailUiState.ErrorKind.Generic -> R.string.pd_generic_error_title to R.string.pd_generic_error_message
    }
    Column(
        modifier = modifier.padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineMedium,
            color = DiscoveryColors.Blue600,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 480.dp),
        )
        Button(
            onClick = { onEvent(ProductDetailUiEvent.Retry) },
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(text = stringResource(R.string.pd_retry))
        }
    }
}

@Composable
private fun BottomBar(
    query: String,
    onEvent: (ProductDetailUiEvent) -> Unit,
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
                text = stringResource(R.string.pd_prompt),
                style = MaterialTheme.typography.titleSmall,
                color = DiscoveryColors.Blue800,
            )
        }
        DiscoverySearchBar(
            query = query,
            onQueryChange = { onEvent(ProductDetailUiEvent.QueryChanged(it)) },
            onSearch = { onEvent(ProductDetailUiEvent.SubmitSearch) },
            placeholder = stringResource(R.string.pd_search_placeholder),
            searchIconContentDescription = stringResource(R.string.pd_search_icon_cd),
            modifier = Modifier.weight(1f),
        )
        MicButton(
            isListening = false,
            compact = true,
            contentDescription = stringResource(R.string.pd_mic_cd),
            onClick = { onEvent(ProductDetailUiEvent.ActivateMic) },
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
        title = { Text(stringResource(R.string.pd_close_dialog_title)) },
        text = { Text(stringResource(R.string.pd_close_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.pd_close_dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pd_close_dialog_dismiss)) }
        },
    )
}

/** Maps an API availability status to a (dot color, label) pair, or null when unknown/absent. */
@Composable
private fun availabilityDisplay(availability: String): Pair<Color, String>? = when (availability.uppercase()) {
    "AVAILABLE" -> DiscoveryColors.Green500 to stringResource(R.string.pd_availability_available)
    "LOW_STOCK" -> DiscoveryColors.MicGradientEnd to stringResource(R.string.pd_availability_low_stock)
    "CONSULT" -> DiscoveryColors.Neutral500 to stringResource(R.string.pd_availability_consult)
    else -> null
}

// --- Previews (landscape kiosk) ---

private fun sampleDetail(imageCount: Int): ProductDetail = ProductDetail(
    id = "p1",
    name = "Taladro Percutor Bosch GSB 13 RE Professional 12V",
    description = "El Bosch GSB 13 RE Professional es el taladro percutor con cable más compacto y " +
        "versátil de la gama azul Bosch. Con 600W de potencia y velocidad variable de 0 a 2,800 rpm, " +
        "se adapta a distintos materiales: desde perforar concreto y mampostería hasta atornillar en " +
        "madera y metal.",
    price = 380.0,
    availability = "AVAILABLE",
    categoryName = "Taladros",
    images = (0 until imageCount).map { ProductImage("img$it.jpg", it, it == 0) },
)

private fun sampleRelated(): ImmutableList<Product> = listOf(
    Product("rel1", "Taladro stilson 14\"", "", 380.0, "AVAILABLE", "Taladros", ""),
    Product("rel2", "Set de brocas (5 piezas)", "", 49.90, "AVAILABLE", "Accesorios", ""),
    Product("rel3", "Lentes de seguridad", "", 19.90, "AVAILABLE", "Seguridad", ""),
).toImmutableList()

@Preview(name = "Detail loaded (3 photos) — light", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailLoadedLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductDetailContent(
            uiState = ProductDetailUiState(
                status = ProductDetailUiState.Status.Loaded(sampleDetail(4), sampleRelated()),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Detail loaded (3 photos) — dark", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailLoadedDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductDetailContent(
            uiState = ProductDetailUiState(
                status = ProductDetailUiState.Status.Loaded(sampleDetail(4), sampleRelated()),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Detail single photo (gray) — light", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailSinglePhotoPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductDetailContent(
            uiState = ProductDetailUiState(
                status = ProductDetailUiState.Status.Loaded(sampleDetail(1), sampleRelated()),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Detail loading — light", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailLoadingPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductDetailContent(uiState = ProductDetailUiState(), onEvent = {})
    }
}

@Preview(name = "Detail not found — light", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailNotFoundPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductDetailContent(
            uiState = ProductDetailUiState(status = ProductDetailUiState.Status.Error(ProductDetailUiState.ErrorKind.NotFound)),
            onEvent = {},
        )
    }
}

@Preview(name = "Detail network error — dark", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailNetworkErrorPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductDetailContent(
            uiState = ProductDetailUiState(status = ProductDetailUiState.Status.Error(ProductDetailUiState.ErrorKind.Network)),
            onEvent = {},
        )
    }
}

@Preview(name = "Detail close dialog — light", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailCloseDialogPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductDetailContent(
            uiState = ProductDetailUiState(
                status = ProductDetailUiState.Status.Loaded(sampleDetail(2), sampleRelated()),
                showCloseConfirmDialog = true,
            ),
            onEvent = {},
        )
    }
}
