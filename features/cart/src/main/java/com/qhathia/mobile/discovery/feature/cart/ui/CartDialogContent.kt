package com.qhathia.mobile.discovery.feature.cart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.qhathia.mobile.discovery.core.domain.model.CartItem
import com.qhathia.mobile.discovery.core.ui.component.CloseIconButton
import com.qhathia.mobile.discovery.core.ui.component.DiscoveryDivider
import com.qhathia.mobile.discovery.core.ui.component.DiscoveryTextField
import com.qhathia.mobile.discovery.core.ui.component.PrimaryActionButton
import com.qhathia.mobile.discovery.core.ui.component.QuantityStepper
import com.qhathia.mobile.discovery.core.ui.component.SubmissionErrorContent
import com.qhathia.mobile.discovery.core.ui.component.SubmissionLine
import com.qhathia.mobile.discovery.core.ui.component.SubmissionSuccessContent
import com.qhathia.mobile.discovery.core.ui.component.SubmittingOverlay
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.cart.R
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEvent
import com.qhathia.mobile.discovery.feature.cart.model.CartUiState
import com.qhathia.mobile.discovery.feature.cart.model.OrderSubmission
import com.qhathia.mobile.discovery.feature.cart.model.OrderedLine
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import com.qhathia.mobile.discovery.core.ui.R as UiR

/**
 * Stateless cart UI — a pure function of [CartUiState], following the cart Figma (node `758-25521`).
 * The [CartUiState.submission] phase decides what is shown: the normal cart (with a blocking loading
 * overlay while [OrderSubmission.Submitting]), the success confirmation summary, or the simplified
 * error screen. Built from the shared `core:ui` design-system components.
 */
@Composable
fun CartDialogContent(
    uiState: CartUiState,
    onEvent: (CartUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val submission = uiState.submission) {
        is OrderSubmission.Success ->
            SubmissionSuccessContent(
                title = stringResource(R.string.cart_success_title),
                subtitle = stringResource(
                    if (submission.shareDelivered) R.string.cart_success_subtitle
                    else R.string.cart_success_share_failed_subtitle,
                ),
                summary = submission.summary.toSubmissionLines(),
                // The "check your phone" prompt only applies when the quote actually reached WhatsApp.
                footnote = if (submission.shareDelivered) stringResource(R.string.cart_success_check_phone) else null,
                actionLabel = stringResource(R.string.cart_success_accept),
                onAction = { onEvent(CartUiEvent.AcceptSuccess) },
                quantityLabel = { pluralStringResource(R.plurals.cart_success_qty, it, it) },
                modifier = modifier,
            )

        OrderSubmission.Error ->
            SubmissionErrorContent(
                title = stringResource(R.string.cart_error_title),
                message = stringResource(R.string.cart_error_message),
                // "Entendido" carries the higher priority (filled) — it returns to the cart to retry.
                primaryLabel = stringResource(R.string.cart_error_understood),
                onPrimary = { onEvent(CartUiEvent.DismissError) },
                secondaryLabel = stringResource(R.string.cart_error_cancel),
                onSecondary = { onEvent(CartUiEvent.CancelOrder) },
                modifier = modifier,
            )

        OrderSubmission.Idle, OrderSubmission.Submitting ->
            CartContent(uiState = uiState, onEvent = onEvent, modifier = modifier)
    }
}

private fun ImmutableList<OrderedLine>.toSubmissionLines(): ImmutableList<SubmissionLine> =
    map { SubmissionLine(imageUrl = it.imageUrl, name = it.name, quantity = it.quantity) }.toImmutableList()

// --- Cart body (idle + submitting overlay) ---

@Composable
private fun CartContent(
    uiState: CartUiState,
    onEvent: (CartUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Header(itemCount = uiState.totalItemCount, onClose = { onEvent(CartUiEvent.Close) })
                DiscoveryDivider()
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
        if (uiState.isSubmitting) {
            SubmittingOverlay(contentDescription = stringResource(R.string.cart_submitting_cd))
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
        CloseIconButton(
            onClick = onClose,
            contentDescription = stringResource(R.string.cart_close_cd),
            size = 48.dp,
            iconSize = 16.dp,
        )
    }
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
                    CloseIconButton(
                        onClick = { onEvent(CartUiEvent.RemoveLine(item.productId)) },
                        contentDescription = stringResource(R.string.cart_line_remove_cd),
                        size = 48.dp,
                        iconSize = 14.dp,
                        cornerRadius = 8.dp,
                        bordered = false,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = stringResource(UiR.string.discovery_price_format, item.priceUnit),
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
                        decreaseContentDescription = stringResource(UiR.string.discovery_cd_qty_decrease),
                        increaseContentDescription = stringResource(UiR.string.discovery_cd_qty_increase),
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
        DiscoveryTextField(
            value = uiState.name,
            onValueChange = { onEvent(CartUiEvent.NameChanged(it)) },
            label = stringResource(R.string.cart_name_label),
            placeholder = stringResource(R.string.cart_name_placeholder),
            isError = uiState.showNameError,
            errorText = if (uiState.showNameError) stringResource(R.string.cart_name_error) else null,
            leadingIconRes = UiR.drawable.ic_user,
            keyboardType = KeyboardType.Text,
        )
        DiscoveryTextField(
            value = uiState.phone,
            onValueChange = { onEvent(CartUiEvent.PhoneChanged(it)) },
            label = stringResource(R.string.cart_phone_label),
            placeholder = stringResource(R.string.cart_phone_placeholder),
            isError = uiState.showPhoneError,
            errorText = if (uiState.showPhoneError) stringResource(R.string.cart_phone_error) else null,
            leadingIconRes = UiR.drawable.ic_phone,
            keyboardType = KeyboardType.Phone,
        )
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
                text = stringResource(UiR.string.discovery_price_format, total),
                style = MaterialTheme.typography.bodySmall,
                color = DiscoveryColors.CatalogText,
            )
        }
        DiscoveryDivider(color = DiscoveryColors.Blue200)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.cart_total_label),
                style = MaterialTheme.typography.titleSmall,
                color = DiscoveryColors.CatalogText,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(UiR.string.discovery_price_format, total),
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
        // Primary: "Realizar pedido" — enabled only when the consumer data is valid.
        PrimaryActionButton(
            text = stringResource(R.string.cart_place_order),
            onClick = { onEvent(CartUiEvent.PlaceOrder) },
            enabled = isOrderEnabled,
            containerColor = if (isOrderEnabled) DiscoveryColors.Blue700 else DiscoveryColors.Divider,
            contentColor = if (isOrderEnabled) Color.White else DiscoveryColors.TextDisabled,
        )
        PrimaryActionButton(
            text = stringResource(R.string.cart_keep_shopping),
            onClick = { onEvent(CartUiEvent.KeepShopping) },
            containerColor = Color.Transparent,
            contentColor = DiscoveryColors.CatalogText,
        )
        PrimaryActionButton(
            text = stringResource(UiR.string.discovery_action_share),
            onClick = { onEvent(CartUiEvent.ShareWhatsApp) },
            containerColor = DiscoveryColors.Green500,
            contentColor = Color.White,
            leadingIconRes = UiR.drawable.ic_whatsapp,
            iconContentDescription = stringResource(R.string.cart_share_whatsapp_cd),
        )
    }
}

private val FOOTER_WIDTH = 420.dp

// --- Previews (landscape kiosk) ---

private fun sampleItems(): kotlinx.collections.immutable.ImmutableList<CartItem> =
    kotlinx.collections.immutable.persistentListOf(
        CartItem("p1", "Taladro Percutor Bosch GSB 13 RE Professional 12V", 189.0, "", "AVAILABLE", 1, 99),
        CartItem("p2", "Set de brocas (5 piezas)", 49.9, "", "LOW_STOCK", 5, 5),
    )

private fun sampleSummary(): kotlinx.collections.immutable.ImmutableList<OrderedLine> =
    kotlinx.collections.immutable.persistentListOf(
        OrderedLine("", "Taladro Percutor Bosch GSB 13 RE Professional 12V", 1),
        OrderedLine("", "Set de brocas (5 piezas)", 5),
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

@androidx.compose.ui.tooling.preview.Preview(name = "Cart — submitting (light)", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun CartSubmittingPreview() {
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
                submission = OrderSubmission.Submitting,
            ),
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Order — success (light)", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun OrderSuccessPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = false) {
        CartDialogContent(
            uiState = CartUiState(submission = OrderSubmission.Success(sampleSummary())),
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Order — success (dark)", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun OrderSuccessDarkPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = true) {
        CartDialogContent(
            uiState = CartUiState(submission = OrderSubmission.Success(sampleSummary())),
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Order — created, share failed (light)", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun OrderShareFailedPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = false) {
        CartDialogContent(
            uiState = CartUiState(submission = OrderSubmission.Success(sampleSummary(), shareDelivered = false)),
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Order — error (light)", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun OrderErrorPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = false) {
        CartDialogContent(
            uiState = CartUiState(submission = OrderSubmission.Error),
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Order — error (dark)", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun OrderErrorDarkPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = true) {
        CartDialogContent(
            uiState = CartUiState(submission = OrderSubmission.Error),
            onEvent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
