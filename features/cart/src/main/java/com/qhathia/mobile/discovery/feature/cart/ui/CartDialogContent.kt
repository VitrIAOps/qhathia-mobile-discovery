package com.qhathia.mobile.discovery.feature.cart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.ui.component.QuantityStepper
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.cart.R
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEvent
import com.qhathia.mobile.discovery.feature.cart.model.CartUiState
import com.qhathia.mobile.discovery.core.ui.R as UiR

/**
 * Stateless cart UI — a pure function of [CartUiState], following the cart Figma (node `758-25521`):
 * a rounded sheet with a "Carrito" header + item-count subtitle and a close control, a divider, and a
 * two-column body: the scrollable product list on the left and a footer panel on the right with the
 * consumer-data form, a receipt-styled totals section, and the order / keep-shopping / share buttons.
 */
@Composable
fun CartDialogContent(
    uiState: CartUiState,
    onEvent: (CartUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(itemCount = uiState.totalItemCount, onClose = { onEvent(CartUiEvent.Close) })
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (uiState.isEmpty) {
                        EmptyState(modifier = Modifier.align(Alignment.Center))
                    } else {
                        ProductList(items = uiState.items, onEvent = onEvent)
                    }
                }
                FooterPanel(uiState = uiState, onEvent = onEvent)
            }
        }
    }
}

// --- Header ---

@Composable
private fun Header(itemCount: Int, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = stringResource(R.string.cart_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = pluralStringResource(R.plurals.cart_subtitle_count, itemCount, itemCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, DiscoveryColors.Divider, RoundedCornerShape(10.dp))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(UiR.drawable.ic_close),
                contentDescription = stringResource(R.string.cart_close_cd),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun HorizontalDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DiscoveryColors.Divider))
}

// --- Product list ---

@Composable
private fun ProductList(
    items: List<CartItem>,
    onEvent: (CartUiEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(items = items, key = { it.productId }) { item ->
            CartLineCard(item = item, onEvent = onEvent)
        }
    }
}

@Composable
private fun CartLineCard(
    item: CartItem,
    onEvent: (CartUiEvent) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DiscoveryColors.Divider),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DiscoveryColors.Neutral50),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEvent(CartUiEvent.RemoveLine(item.productId)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(UiR.drawable.ic_close),
                            contentDescription = stringResource(R.string.cart_line_remove_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = stringResource(R.string.cart_price, item.priceUnit),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.cart_go_to_detail),
                        style = MaterialTheme.typography.labelLarge,
                        color = DiscoveryColors.CatalogText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEvent(CartUiEvent.OpenLineDetail(item.productId)) }
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    )
                    QuantityStepper(
                        quantity = item.quantity,
                        onDecrease = { onEvent(CartUiEvent.DecreaseLine(item.productId)) },
                        onIncrease = { onEvent(CartUiEvent.IncreaseLine(item.productId)) },
                        decreaseContentDescription = stringResource(R.string.cart_qty_decrease_cd),
                        increaseContentDescription = stringResource(R.string.cart_qty_increase_cd),
                        canIncrease = item.quantity < item.maxQuantity,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.cart_empty_message),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.cart_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// --- Footer panel: form + totals + actions ---

@Composable
private fun FooterPanel(
    uiState: CartUiState,
    onEvent: (CartUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(FOOTER_WIDTH)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        ConsumerForm(uiState = uiState, onEvent = onEvent)
        TotalsSection(itemCount = uiState.totalItemCount, total = uiState.totalAmount)
        ActionButtons(isOrderEnabled = uiState.isOrderEnabled, onEvent = onEvent)
    }
}

@Composable
private fun ConsumerForm(
    uiState: CartUiState,
    onEvent: (CartUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.cart_form_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FormField(
            label = stringResource(R.string.cart_name_label),
            value = uiState.name,
            placeholder = stringResource(R.string.cart_name_placeholder),
            iconRes = UiR.drawable.ic_user,
            errorText = if (uiState.showNameError) stringResource(R.string.cart_name_error) else null,
            keyboardType = KeyboardType.Text,
            onValueChange = { onEvent(CartUiEvent.NameChanged(it)) },
        )
        FormField(
            label = stringResource(R.string.cart_phone_label),
            value = uiState.phone,
            placeholder = stringResource(R.string.cart_phone_placeholder),
            iconRes = UiR.drawable.ic_phone,
            errorText = if (uiState.showPhoneError) stringResource(R.string.cart_phone_error) else null,
            keyboardType = KeyboardType.Phone,
            onValueChange = { onEvent(CartUiEvent.PhoneChanged(it)) },
        )
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    placeholder: String,
    iconRes: Int,
    errorText: String?,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = DiscoveryColors.TextMuted)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = if (errorText != null) MaterialTheme.colorScheme.error else DiscoveryColors.Divider,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (errorText != null) {
            Text(text = errorText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun TotalsSection(itemCount: Int, total: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DiscoveryColors.Blue50)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pluralStringResource(R.plurals.cart_subtotal_label, itemCount, itemCount),
                style = MaterialTheme.typography.bodySmall,
                color = DiscoveryColors.CatalogText,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.cart_price, total),
                style = MaterialTheme.typography.bodySmall,
                color = DiscoveryColors.CatalogText,
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DiscoveryColors.Blue200))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.cart_total_label),
                style = MaterialTheme.typography.titleSmall,
                color = DiscoveryColors.CatalogText,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.cart_price, total),
                style = MaterialTheme.typography.headlineMedium,
                color = DiscoveryColors.CatalogText,
            )
        }
    }
}

@Composable
private fun ActionButtons(
    isOrderEnabled: Boolean,
    onEvent: (CartUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        // Primary: "Realizar pedido" — render-only (no order API yet); enabled only when data is valid.
        FilledActionButton(
            text = stringResource(R.string.cart_place_order),
            container = if (isOrderEnabled) DiscoveryColors.Blue700 else DiscoveryColors.Divider,
            contentColor = if (isOrderEnabled) Color.White else DiscoveryColors.TextDisabled,
            enabled = isOrderEnabled,
            onClick = { onEvent(CartUiEvent.PlaceOrder) },
        )
        FilledActionButton(
            text = stringResource(R.string.cart_keep_shopping),
            container = Color.Transparent,
            contentColor = DiscoveryColors.CatalogText,
            enabled = true,
            onClick = { onEvent(CartUiEvent.KeepShopping) },
        )
        FilledActionButton(
            text = stringResource(R.string.cart_share_whatsapp),
            container = DiscoveryColors.Green500,
            contentColor = Color.White,
            enabled = true,
            iconRes = UiR.drawable.ic_whatsapp,
            iconContentDescription = stringResource(R.string.cart_share_whatsapp_cd),
            onClick = { onEvent(CartUiEvent.ShareWhatsApp) },
        )
    }
}

@Composable
private fun FilledActionButton(
    text: String,
    container: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    iconRes: Int? = null,
    iconContentDescription: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = contentColor)
        if (iconRes != null) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            Icon(
                painter = painterResource(iconRes),
                contentDescription = iconContentDescription,
                tint = contentColor,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private val FOOTER_WIDTH = 420.dp

// --- Previews (landscape kiosk) ---

private fun sampleItems(): kotlinx.collections.immutable.ImmutableList<CartItem> =
    kotlinx.collections.immutable.persistentListOf(
        CartItem("p1", "Taladro Percutor Bosch GSB 13 RE Professional 12V", 189.0, "", "AVAILABLE", 1, 99),
        CartItem("p2", "Set de brocas (5 piezas)", 49.9, "", "LOW_STOCK", 5, 5),
    )

@androidx.compose.ui.tooling.preview.Preview(name = "Cart — valid (light)", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun CartValidPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = false) {
        CartDialogContent(
            uiState = CartUiState(
                items = sampleItems(),
                totalItemCount = 6,
                totalAmount = 438.5,
                name = "Ana Pérez",
                phone = "987654321",
                nameError = null,
                phoneError = null,
                isOrderEnabled = true,
            ),
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Cart — invalid (dark)", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun CartInvalidPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = true) {
        CartDialogContent(
            uiState = CartUiState(
                items = sampleItems(),
                totalItemCount = 6,
                totalAmount = 438.5,
                name = "A",
                phone = "123",
                nameError = com.qhathia.mobile.discovery.core.domain.usecase.ConsumerNameError.NoReferentialName,
                phoneError = com.qhathia.mobile.discovery.core.domain.usecase.ConsumerPhoneError.InvalidFormat,
                isOrderEnabled = false,
            ),
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Cart — empty (light)", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun CartEmptyPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = false) {
        CartDialogContent(
            uiState = CartUiState(),
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
