package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.R
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing

/**
 * Compact notice for the top bar while the basket holds the most distinct products it can: says why
 * the add buttons look unavailable before the customer taps one, with a way to the cart (D17-03). It
 * lives in the top bar's free space so the product grid keeps its full height.
 *
 * @param onOpenCart invoked by "Ver carrito".
 */
@Composable
fun BasketFullNotice(
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, DiscoveryColors.Divider),
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.discovery_cart_full_short),
                style = MaterialTheme.typography.bodyMedium,
                color = DiscoveryColors.CatalogText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(onClick = onOpenCart) {
                Text(
                    text = stringResource(R.string.discovery_cart_view),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = DiscoveryColors.Blue700,
                )
            }
        }
    }
}

@Preview(name = "BasketFullNotice — light", showBackground = true, backgroundColor = 0xFFF8F8F8, widthDp = 480)
@Composable
private fun BasketFullNoticeLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        BasketFullNotice(onOpenCart = {})
    }
}

@Preview(name = "BasketFullNotice — dark", showBackground = true, backgroundColor = 0xFF122842, widthDp = 480)
@Composable
private fun BasketFullNoticeDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        BasketFullNotice(onOpenCart = {})
    }
}
