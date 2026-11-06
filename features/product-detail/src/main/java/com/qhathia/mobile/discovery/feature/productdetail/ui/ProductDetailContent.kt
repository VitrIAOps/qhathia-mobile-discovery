package com.qhathia.mobile.discovery.feature.productdetail.ui

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.domain.CartConstants
import com.qhathia.mobile.discovery.core.domain.model.Product
import com.qhathia.mobile.discovery.core.domain.model.ProductDetail
import com.qhathia.mobile.discovery.core.domain.model.ProductImage
import com.qhathia.mobile.discovery.core.domain.usecase.isOrderable
import com.qhathia.mobile.discovery.core.ui.component.CartButton
import com.qhathia.mobile.discovery.core.ui.component.ConfirmationDialog
import com.qhathia.mobile.discovery.core.ui.component.closeDialogMessage
import com.qhathia.mobile.discovery.core.ui.component.DiscoveryDivider
import com.qhathia.mobile.discovery.core.ui.component.DiscoverySearchBar
import com.qhathia.mobile.discovery.core.ui.component.MicButton
import com.qhathia.mobile.discovery.core.ui.component.PrimaryActionButton
import com.qhathia.mobile.discovery.core.ui.component.ProductImageGallery
import com.qhathia.mobile.discovery.core.ui.component.ProductRowCard
import com.qhathia.mobile.discovery.core.ui.component.QuantityStepper
import com.qhathia.mobile.discovery.core.ui.component.labeled
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
                cartItemCount = uiState.cartItemCount,
                onBack = { onEvent(ProductDetailUiEvent.Back) },
                onNewSearch = { onEvent(ProductDetailUiEvent.NewSearch) },
                onCart = { onEvent(ProductDetailUiEvent.OpenCart) },
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (val status = uiState.status) {
                    ProductDetailUiState.Status.Loading ->
                        LoadingBody(modifier = Modifier.align(Alignment.Center))

                    is ProductDetailUiState.Status.Loaded ->
                        LoadedBody(
                            detail = status.detail,
                            related = uiState.relatedProducts,
                            quantity = uiState.quantity,
                            onEvent = onEvent,
                        )

                    is ProductDetailUiState.Status.Error ->
                        ErrorBody(kind = status.kind, onEvent = onEvent, modifier = Modifier.align(Alignment.Center))
                }
            }

            // Business rule: keep the bottom chat-area (prompt + search bar + microphone) on the detail screen.
            BottomBar(query = uiState.query, onEvent = onEvent)
        }

        if (uiState.showCloseConfirmDialog) {
            CloseConfirmDialog(
                cartItemCount = uiState.cartItemCount,
                onConfirm = { onEvent(ProductDetailUiEvent.ConfirmCloseSearch) },
                onDismiss = { onEvent(ProductDetailUiEvent.DismissDialog) },
            )
        }

        if (uiState.share.visible) {
            ShareProductDialog(state = uiState.share, onEvent = onEvent)
        }
    }
}

// --- Top bar (mirrors the product-search chrome; re-created here as features can't depend on each other) ---

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
            contentDescription = stringResource(R.string.pd_back_cd),
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
            icon(); label()
        } else {
            label(); icon()
        }
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
    related: ProductDetailUiState.RelatedProducts,
    quantity: Int,
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
                productName = detail.name,
            )
            ProductInfo(detail = detail, quantity = quantity, onEvent = onEvent, modifier = Modifier.weight(1f))
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
    quantity: Int,
    onEvent: (ProductDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            text = detail.name,
            style = MaterialTheme.typography.titleSmall,
            color = DiscoveryColors.CatalogText,
        )
        StockRow(availability = detail.availability, stock = detail.stock)
        // An inactive or non-AVAILABLE product is still shown, but cannot be added or shared (DET-013).
        if (!detail.isOrderable) NotOrderableNote()
        DetailPrice(price = detail.price, discountedPrice = detail.discountedPrice)
        DiscoveryDivider()
        Text(
            text = detail.description,
            style = MaterialTheme.typography.bodySmall,
            color = DiscoveryColors.TextMuted,
        )
        Spacer(modifier = Modifier.size(Spacing.xs))
        ActionRow(
            quantity = quantity,
            maxQuantity = CartConstants.maxQuantityFor(detail.availability, detail.stock),
            orderable = detail.isOrderable,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun StockRow(availability: String, stock: Int?) {
    val (dotColor, label) = availabilityDisplay(availability, stock) ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = dotColor)
    }
}

@Composable
private fun NotOrderableNote() {
    Text(
        text = stringResource(R.string.pd_not_orderable_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun ActionRow(
    quantity: Int,
    maxQuantity: Int,
    orderable: Boolean,
    onEvent: (ProductDetailUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuantityStepper(
            quantity = quantity,
            onDecrease = { onEvent(ProductDetailUiEvent.DecreaseQuantity) },
            onIncrease = { onEvent(ProductDetailUiEvent.IncreaseQuantity) },
            decreaseContentDescription = stringResource(UiR.string.discovery_cd_qty_decrease),
            increaseContentDescription = stringResource(UiR.string.discovery_cd_qty_increase),
            canDecrease = orderable && quantity > 1,
            canIncrease = orderable && quantity < maxQuantity,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ActionButton(
                text = stringResource(UiR.string.discovery_action_add_to_cart),
                iconRes = UiR.drawable.ic_cart,
                container = DiscoveryColors.Blue700,
                enabled = orderable,
                onClick = { onEvent(ProductDetailUiEvent.AddToCart) },
            )
            ActionButton(
                text = stringResource(UiR.string.discovery_action_share),
                iconRes = UiR.drawable.ic_whatsapp,
                container = DiscoveryColors.Green500,
                enabled = orderable,
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
    enabled: Boolean,
    onClick: () -> Unit,
) {
    PrimaryActionButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        containerColor = if (enabled) container else DiscoveryColors.Divider,
        contentColor = if (enabled) Color.White else DiscoveryColors.TextDisabled,
        leadingIconRes = iconRes,
        fillMaxWidth = false,
    )
}

@Composable
private fun RelatedProductsSection(
    related: ProductDetailUiState.RelatedProducts,
    onEvent: (ProductDetailUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        HeadingRow(text = stringResource(R.string.pd_related_headline))
        when (related) {
            ProductDetailUiState.RelatedProducts.Loading -> RelatedProductsPlaceholderBox {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            is ProductDetailUiState.RelatedProducts.Loaded ->
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    related.items.forEach { product ->
                        key(product.id) {
                            ProductRowCard(
                                name = product.name,
                                categoryName = product.categoryName,
                                priceText = stringResource(
                                    UiR.string.discovery_price_format,
                                    product.discountedPrice ?: product.price,
                                ),
                                originalPriceText = product.discountedPrice?.let {
                                    stringResource(UiR.string.discovery_price_format, product.price)
                                },
                                imageUrl = product.imageUrl,
                                cartContentDescription = stringResource(UiR.string.discovery_cd_add_to_cart),
                                onClick = { onEvent(ProductDetailUiEvent.RelatedProductTapped(product.id)) },
                                onAddToCart = { onEvent(ProductDetailUiEvent.RelatedProductAddToCart(product.id)) },
                                cartEnabled = product.isOrderable,
                            )
                        }
                    }
                }

            ProductDetailUiState.RelatedProducts.Empty -> RelatedProductsPlaceholderBox {
                Text(
                    text = stringResource(R.string.pd_related_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DiscoveryColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }
    }
}

/** A box matching the related-products strip height, centering its [content] (empty / loading states). */
@Composable
private fun RelatedProductsPlaceholderBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(RELATED_STRIP_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private val RELATED_STRIP_HEIGHT = 120.dp

@Composable
private fun ErrorBody(
    kind: ProductDetailUiState.ErrorKind,
    onEvent: (ProductDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (titleRes, messageRes) = when (kind) {
        ProductDetailUiState.ErrorKind.NotFound -> R.string.pd_not_found_title to R.string.pd_not_found_message
        ProductDetailUiState.ErrorKind.Network -> UiR.string.discovery_error_network_title to UiR.string.discovery_error_network_message
        ProductDetailUiState.ErrorKind.Generic -> UiR.string.discovery_error_generic_title to R.string.pd_generic_error_message
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
        PrimaryActionButton(
            text = stringResource(UiR.string.discovery_action_retry),
            onClick = { onEvent(ProductDetailUiEvent.Retry) },
            fillMaxWidth = false,
        )
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
                text = stringResource(UiR.string.discovery_search_prompt),
                style = MaterialTheme.typography.titleSmall,
                color = DiscoveryColors.Blue800,
            )
        }
        DiscoverySearchBar(
            query = query,
            onQueryChange = { onEvent(ProductDetailUiEvent.QueryChanged(it)) },
            onSearch = { onEvent(ProductDetailUiEvent.SubmitSearch) },
            placeholder = stringResource(UiR.string.discovery_search_placeholder),
            searchIconContentDescription = stringResource(UiR.string.discovery_cd_search),
            modifier = Modifier.weight(1f),
        )
        MicButton(
            isListening = false,
            compact = true,
            contentDescription = stringResource(UiR.string.discovery_cd_voice_search),
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
    cartItemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(R.string.pd_close_dialog_title),
        message = closeDialogMessage(cartItemCount, R.string.pd_close_dialog_message),
        confirmLabel = stringResource(UiR.string.discovery_dialog_confirm_close),
        dismissLabel = stringResource(UiR.string.discovery_dialog_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

/**
 * Maps the availability and stock to a (dot color, label) pair, or null when unknown/absent. The stock
 * wins when known: 0 (or `OUT_OF_STOCK`) is "Agotado", and up to [CartConstants.LOW_STOCK_LABEL_MAX]
 * units is "Pocas unidades (quedan N)" even when the server says `AVAILABLE` (order-price-check D8).
 */
@Composable
private fun availabilityDisplay(availability: String, stock: Int?): Pair<Color, String>? = when {
    stock == 0 || availability.equals("OUT_OF_STOCK", ignoreCase = true) ->
        MaterialTheme.colorScheme.error to stringResource(R.string.pd_availability_out_of_stock)
    stock != null && stock <= CartConstants.LOW_STOCK_LABEL_MAX ->
        DiscoveryColors.MicGradientEnd to pluralStringResource(R.plurals.pd_availability_low_stock_count, stock, stock)
    else -> availabilityDisplay(availability)
}

/** Maps an API availability status to a (dot color, label) pair, or null when unknown/absent. */
@Composable
private fun availabilityDisplay(availability: String): Pair<Color, String>? = when (availability.uppercase()) {
    "AVAILABLE" -> DiscoveryColors.Green500 to stringResource(R.string.pd_availability_available)
    "LOW_STOCK" -> DiscoveryColors.MicGradientEnd to stringResource(R.string.pd_availability_low_stock)
    "CONSULT" -> DiscoveryColors.Neutral500 to stringResource(R.string.pd_availability_consult)
    else -> null
}

/**
 * The detail price. With an active discount the server-computed [discountedPrice] is shown highlighted
 * and the list [price] struck through next to it; otherwise only [price].
 */
@Composable
private fun DetailPrice(
    price: Double,
    discountedPrice: Double?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = stringResource(UiR.string.discovery_price_format, discountedPrice ?: price),
            style = MaterialTheme.typography.headlineMedium,
            color = if (discountedPrice != null) DiscoveryColors.AccentOrange else DiscoveryColors.CatalogText,
        )
        if (discountedPrice != null) {
            Text(
                text = stringResource(UiR.string.discovery_price_format, price),
                style = MaterialTheme.typography.bodyLarge,
                color = DiscoveryColors.Neutral500,
                textDecoration = TextDecoration.LineThrough,
            )
        }
    }
}

// --- Previews (landscape kiosk) ---

private fun sampleDetail(
    imageCount: Int,
    discountedPrice: Double? = null,
    availability: String = "AVAILABLE",
    active: Boolean? = true,
    stock: Int? = null,
): ProductDetail = ProductDetail(
    id = "p1",
    name = "Taladro Percutor Bosch GSB 13 RE Professional 12V",
    description = "El Bosch GSB 13 RE Professional es el taladro percutor con cable más compacto y " +
        "versátil de la gama azul Bosch. Con 600W de potencia y velocidad variable de 0 a 2,800 rpm, " +
        "se adapta a distintos materiales: desde perforar concreto y mampostería hasta atornillar en " +
        "madera y metal.",
    price = 380.0,
    availability = availability,
    categoryName = "Taladros",
    images = (0 until imageCount).map { ProductImage("img$it.jpg", it, it == 0) },
    discountedPrice = discountedPrice,
    active = active,
    stock = stock,
)

private fun sampleRelated(): ImmutableList<Product> = listOf(
    Product("rel1", "Taladro stilson 14\"", "", 380.0, "AVAILABLE", "Taladros", "", discountedPrice = 340.0),
    Product("rel2", "Set de brocas (5 piezas)", "", 49.90, "LOW_STOCK", "Accesorios", ""),
    Product("rel3", "Lentes de seguridad", "", 19.90, "AVAILABLE", "Seguridad", ""),
).toImmutableList()

private fun loadedState(
    imageCount: Int,
    related: ProductDetailUiState.RelatedProducts = ProductDetailUiState.RelatedProducts.Loaded(sampleRelated()),
    showCloseConfirmDialog: Boolean = false,
    discountedPrice: Double? = null,
    active: Boolean? = true,
    cartItemCount: Int = 0,
    stock: Int? = null,
): ProductDetailUiState = ProductDetailUiState(
    status = ProductDetailUiState.Status.Loaded(sampleDetail(imageCount, discountedPrice, active = active, stock = stock)),
    relatedProducts = related,
    showCloseConfirmDialog = showCloseConfirmDialog,
    cartItemCount = cartItemCount,
)

@Preview(name = "Detail loaded (related) — light", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailLoadedLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductDetailContent(uiState = loadedState(imageCount = 4), onEvent = {})
    }
}

@Preview(name = "Detail loaded (related) — dark", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailLoadedDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductDetailContent(uiState = loadedState(imageCount = 4), onEvent = {})
    }
}

@Preview(name = "Detail few units left — light", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailLowStockLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductDetailContent(uiState = loadedState(imageCount = 4, stock = 3), onEvent = {})
    }
}

@Preview(name = "Detail few units left — dark", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailLowStockDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductDetailContent(uiState = loadedState(imageCount = 4, stock = 3), onEvent = {})
    }
}

@Preview(name = "Detail sold out — light", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailSoldOutLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductDetailContent(uiState = loadedState(imageCount = 4, stock = 0), onEvent = {})
    }
}

@Preview(name = "Detail sold out — dark", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailSoldOutDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductDetailContent(uiState = loadedState(imageCount = 4, stock = 0), onEvent = {})
    }
}

@Preview(name = "Detail not orderable — light", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailNotOrderableLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductDetailContent(uiState = loadedState(imageCount = 4, active = false), onEvent = {})
    }
}

@Preview(name = "Detail not orderable — dark", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailNotOrderableDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductDetailContent(uiState = loadedState(imageCount = 4, active = false), onEvent = {})
    }
}

@Preview(name = "Detail discounted — light", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailDiscountedLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductDetailContent(uiState = loadedState(imageCount = 4, discountedPrice = 320.0), onEvent = {})
    }
}

@Preview(name = "Detail discounted — dark", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailDiscountedDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductDetailContent(uiState = loadedState(imageCount = 4, discountedPrice = 320.0), onEvent = {})
    }
}

@Preview(name = "Detail loaded (no related) — light", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailRelatedEmptyPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductDetailContent(
            uiState = loadedState(imageCount = 4, related = ProductDetailUiState.RelatedProducts.Empty),
            onEvent = {},
        )
    }
}

@Preview(name = "Detail single photo (gray) — light", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailSinglePhotoPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductDetailContent(uiState = loadedState(imageCount = 1), onEvent = {})
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
            uiState = loadedState(imageCount = 2, showCloseConfirmDialog = true),
            onEvent = {},
        )
    }
}

@Preview(name = "Detail close dialog, 1 in cart — light", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailCloseDialogWithCartLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductDetailContent(
            uiState = loadedState(imageCount = 2, showCloseConfirmDialog = true, cartItemCount = 1),
            onEvent = {},
        )
    }
}

@Preview(name = "Detail close dialog, 1 in cart — dark", showBackground = true, widthDp = 1024, heightDp = 720)
@Composable
private fun DetailCloseDialogWithCartDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductDetailContent(
            uiState = loadedState(imageCount = 2, showCloseConfirmDialog = true, cartItemCount = 1),
            onEvent = {},
        )
    }
}
