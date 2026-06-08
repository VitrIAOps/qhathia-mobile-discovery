package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.R
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing

/**
 * Shared top-bar cart control: a brand-gradient pill with a [label], the cart icon, and a count badge
 * showing [itemCount] when greater than zero. Used identically by product-search and product-detail so
 * the cart chrome stays consistent. Domain-free — the caller passes localized strings.
 *
 * @param itemCount total items in the cart; the badge is hidden when this is 0.
 * @param label the localized button label (e.g. "Ir al carrito").
 * @param contentDescription localized accessibility label (should convey the action and the count).
 * @param onClick invoked when the control is tapped.
 */
@Composable
fun CartButton(
    itemCount: Int,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(DiscoveryColors.MicGradientStart, DiscoveryColors.MicGradientEnd),
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
        BadgedBox(
            badge = {
                if (itemCount > 0) {
                    Badge(
                        containerColor = Color.White,
                        contentColor = DiscoveryColors.MicGradientStart,
                    ) {
                        Text(text = itemCount.toString(), style = MaterialTheme.typography.labelLarge)
                    }
                }
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cart),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview(name = "CartButton (count) — light", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun CartButtonCountPreview() {
    DiscoveryTheme(darkTheme = false) {
        CartButton(itemCount = 3, label = "Ir al carrito", contentDescription = "Ir al carrito, 3 productos", onClick = {})
    }
}

@Preview(name = "CartButton (empty) — dark", showBackground = true, backgroundColor = 0xFF0D1F38)
@Composable
private fun CartButtonEmptyPreview() {
    DiscoveryTheme(darkTheme = true) {
        CartButton(itemCount = 0, label = "Ir al carrito", contentDescription = "Ir al carrito", onClick = {})
    }
}
