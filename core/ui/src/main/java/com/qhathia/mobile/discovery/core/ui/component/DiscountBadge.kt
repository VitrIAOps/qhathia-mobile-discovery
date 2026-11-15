package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.R
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import kotlin.math.roundToInt

/**
 * Rounded percentage off between [price] and [discountedPrice] ("20" for 199.00 → 159.20), or `null`
 * when there is no real discount.
 */
fun discountPercent(price: Double, discountedPrice: Double?): Int? {
    if (discountedPrice == null || price <= 0.0 || discountedPrice >= price) return null
    return ((1 - discountedPrice / price) * 100).roundToInt().takeIf { it > 0 }
}

/**
 * "-20 %" chip for a discounted product (UX-T-19). Decorative for screen readers: the card or price
 * label already says the price before the discount.
 */
@Composable
fun DiscountBadge(percent: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.discovery_discount_badge, percent),
        style = MaterialTheme.typography.labelLarge,
        color = Color.White,
        modifier = modifier
            .clearAndSetSemantics {}
            .background(DiscoveryColors.AccentOrange, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Preview(name = "DiscountBadge", showBackground = true)
@Composable
private fun DiscountBadgePreview() {
    DiscoveryTheme(darkTheme = false) { DiscountBadge(percent = 20) }
}
