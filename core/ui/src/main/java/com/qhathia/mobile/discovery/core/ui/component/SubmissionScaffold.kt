package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * One product row for a submission summary (order-placed or share-sent confirmation), captured at
 * submit time so it survives the basket being cleared.
 *
 * @property imageUrl the product's primary image URL, or empty when none.
 * @property name the product display name.
 * @property quantity the submitted quantity.
 * @property priceText the line total as the server recorded it, already formatted ("S/ 160.00"), or
 *   `null` to show only the quantity.
 */
data class SubmissionLine(
    val imageUrl: String,
    val name: String,
    val quantity: Int,
    val priceText: String? = null,
)

/**
 * Full-surface blocking overlay shown while a request is in flight. A dimmed scrim (an API-agnostic
 * stand-in for a blur, since `Modifier.blur` needs API 31+ and minSdk is 26) intercepts every pointer
 * event so the underlying form and buttons cannot be touched, and a centered loader announces the
 * loading state to assistive technology via [contentDescription].
 *
 * Shared by the cart order/share flow and the product-detail share flow so both render the identical
 * blocking-loading UX.
 */
@Composable
fun SubmittingOverlay(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA))
            // Consume every pointer event so nothing below the overlay is reachable during submission.
            // Not a clickable: it is not a control, so accessibility services do not list it.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = Color.White,
            modifier = Modifier
                .size(48.dp)
                .semantics { stateDescription = contentDescription },
        )
    }
}

/**
 * The success confirmation screen shared by the cart order/share and product-detail share flows: a
 * green [title], a [subtitle], the [summary] of submitted products, an optional [footnote] (e.g. the
 * "check your phone" prompt — pass `null` to hide it), and a single accept action.
 *
 * @param quantityLabel maps a line quantity to its localized label (the caller owns the plural copy).
 * @param totalText the recorded total row, already formatted ("Total del pedido  S/ 80.00"), or `null`.
 * @param note a warning shown under the total (e.g. the recorded total differs from the cart), or `null`.
 */
@Composable
fun SubmissionSuccessContent(
    title: String,
    subtitle: String,
    summary: ImmutableList<SubmissionLine>,
    footnote: String?,
    actionLabel: String,
    onAction: () -> Unit,
    quantityLabel: @Composable (Int) -> String,
    modifier: Modifier = Modifier,
    totalText: String? = null,
    note: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = DiscoveryColors.Green500,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = PANEL_MAX_WIDTH),
            )
            SubmissionSummaryList(summary = summary, quantityLabel = quantityLabel)
            if (totalText != null) {
                Text(
                    text = totalText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(max = PANEL_MAX_WIDTH).fillMaxWidth(),
                )
            }
            if (note != null) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = PANEL_MAX_WIDTH),
                )
            }
            if (footnote != null) {
                Text(
                    text = footnote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = PANEL_MAX_WIDTH),
                )
            }
            PrimaryActionButton(
                text = actionLabel,
                onClick = onAction,
                containerColor = DiscoveryColors.Blue700,
                contentColor = Color.White,
                modifier = Modifier.widthIn(max = PANEL_MAX_WIDTH),
            )
        }
    }
}

@Composable
private fun SubmissionSummaryList(
    summary: ImmutableList<SubmissionLine>,
    quantityLabel: @Composable (Int) -> String,
) {
    Column(
        modifier = Modifier
            .widthIn(max = PANEL_MAX_WIDTH)
            .heightIn(max = SUMMARY_MAX_HEIGHT)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        summary.forEach { line -> SubmissionLineRow(line = line, quantityLabel = quantityLabel) }
    }
}

@Composable
private fun SubmissionLineRow(
    line: SubmissionLine,
    quantityLabel: @Composable (Int) -> String,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, DiscoveryColors.Divider),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductImage(
                url = line.imageUrl,
                contentDescription = null, // the name next to it is the accessible label
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = line.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = quantityLabel(line.quantity),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (line.priceText != null) {
                Text(
                    text = line.priceText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * The simplified error-recovery screen shared by the cart order/share and product-detail share flows:
 * a [title], a [message], a higher-priority [primaryLabel] action (filled) and an optional
 * lower-priority [secondaryLabel] action (text), left out when `null`. The caller decides what each
 * action does (retry vs. cancel).
 */
@Composable
fun SubmissionErrorContent(
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String?,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = PANEL_MAX_WIDTH),
            )
            PrimaryActionButton(
                text = primaryLabel,
                onClick = onPrimary,
                containerColor = DiscoveryColors.Blue700,
                contentColor = Color.White,
                modifier = Modifier.widthIn(max = PANEL_MAX_WIDTH),
            )
            if (secondaryLabel != null) {
                PrimaryActionButton(
                    text = secondaryLabel,
                    onClick = onSecondary,
                    containerColor = Color.Transparent,
                    contentColor = DiscoveryColors.CatalogText,
                    modifier = Modifier.widthIn(max = PANEL_MAX_WIDTH),
                )
            }
        }
    }
}

private val PANEL_MAX_WIDTH = 480.dp
private val SUMMARY_MAX_HEIGHT = 260.dp
private const val SCRIM_ALPHA = 0.45f

@Preview(name = "Success with total — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun SubmissionSuccessTotalLightPreview() {
    DiscoveryTheme(darkTheme = false) { SubmissionSuccessTotalSample(note = null) }
}

@Preview(name = "Success with total and note — dark", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun SubmissionSuccessTotalDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        SubmissionSuccessTotalSample(note = "El total registrado es S/ 80.00 (en tu carrito viste S/ 100.00). Confírmalo con el vendedor.")
    }
}

@Composable
private fun SubmissionSuccessTotalSample(note: String?) {
    SubmissionSuccessContent(
        title = "¡Pedido generado con éxito!",
        subtitle = "Acércate al vendedor para recibir tu pedido y realizar el pago.",
        summary = persistentListOf(
            SubmissionLine(imageUrl = "", name = "Pulidora angular 4 1/2\"", quantity = 2, priceText = "S/ 160.00"),
            SubmissionLine(imageUrl = "", name = "Disco de corte", quantity = 1, priceText = "S/ 12.50"),
        ),
        footnote = "Revisa tu celular para ver la cotización.",
        actionLabel = "Aceptar",
        onAction = {},
        quantityLabel = { "Cantidad: $it" },
        totalText = "Total del pedido  S/ 172.50",
        note = note,
    )
}
