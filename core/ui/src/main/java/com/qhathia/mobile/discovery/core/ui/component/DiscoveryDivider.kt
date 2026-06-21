package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme

/**
 * A full-width hairline divider. Replaces the `Box(Modifier.fillMaxWidth().height(1.dp).background(...))`
 * idiom that was duplicated across the cart, product-detail and settings screens.
 *
 * @param color the line color; defaults to the design-system [DiscoveryColors.Divider]. Pass e.g.
 *   [DiscoveryColors.Blue200] for the receipt/panel dividers.
 * @param thickness the line height; defaults to 1dp.
 */
@Composable
fun DiscoveryDivider(
    modifier: Modifier = Modifier,
    color: Color = DiscoveryColors.Divider,
    thickness: Dp = 1.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color),
    )
}

@Preview(name = "DiscoveryDivider — light", showBackground = true, widthDp = 240)
@Composable
private fun DiscoveryDividerLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        DiscoveryDivider()
    }
}

@Preview(name = "DiscoveryDivider — dark", showBackground = true, widthDp = 240, backgroundColor = 0xFF0D1F38)
@Composable
private fun DiscoveryDividerDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        DiscoveryDivider(color = DiscoveryColors.Blue200)
    }
}
