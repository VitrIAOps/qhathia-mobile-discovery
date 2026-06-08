package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme

/**
 * Reusable quantity stepper pill (− / qty / +) shared by the product-detail action row and the cart
 * dialog lines. The caller owns the quantity and the bounds; the stepper only renders and reports
 * taps. Disabled controls are dimmed and do not invoke their callback.
 *
 * @param quantity the current quantity to display.
 * @param onDecrease invoked when the − control is tapped (only when [canDecrease]).
 * @param onIncrease invoked when the + control is tapped (only when [canIncrease]).
 * @param decreaseContentDescription localized accessibility label for the − control.
 * @param increaseContentDescription localized accessibility label for the + control.
 * @param canDecrease whether the − control is enabled.
 * @param canIncrease whether the + control is enabled.
 */
@Composable
fun QuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseContentDescription: String,
    increaseContentDescription: String,
    modifier: Modifier = Modifier,
    canDecrease: Boolean = true,
    canIncrease: Boolean = true,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, DiscoveryColors.SearchBorder, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            symbol = "−", // minus sign
            contentDescription = decreaseContentDescription,
            enabled = canDecrease,
            onClick = onDecrease,
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
            contentDescription = increaseContentDescription,
            enabled = canIncrease,
            onClick = onIncrease,
        )
    }
}

@Composable
private fun StepperButton(
    symbol: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else DiscoveryColors.TextDisabled,
        )
    }
}

@Preview(name = "QuantityStepper — light", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun QuantityStepperPreview() {
    DiscoveryTheme(darkTheme = false) {
        QuantityStepper(
            quantity = 2,
            onDecrease = {},
            onIncrease = {},
            decreaseContentDescription = "Quitar una unidad",
            increaseContentDescription = "Agregar una unidad",
        )
    }
}

@Preview(name = "QuantityStepper (max) — dark", showBackground = true, backgroundColor = 0xFF122842)
@Composable
private fun QuantityStepperMaxPreview() {
    DiscoveryTheme(darkTheme = true) {
        QuantityStepper(
            quantity = 5,
            onDecrease = {},
            onIncrease = {},
            decreaseContentDescription = "Quitar una unidad",
            increaseContentDescription = "Agregar una unidad",
            canIncrease = false,
        )
    }
}
