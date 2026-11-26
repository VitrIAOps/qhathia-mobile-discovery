package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.R
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors

/**
 * The round "Agregar al carrito" button of the product cards: a 48 dp touch target around a 40 dp
 * circle, so a tap near its edge adds to the cart instead of opening the detail below it (UX-X-03).
 *
 * @param enabled `false` when the product cannot be ordered: dimmed and does nothing.
 * @param dimmed only looks dimmed (a full basket, F-33): it still takes the tap, so the screen can say
 *   why the product was not added, and it is not announced as disabled.
 */
@Composable
internal fun CartCircleButton(
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .labeled(contentDescription),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .alpha(if (enabled && !dimmed) 1f else DISABLED_ALPHA)
                .clip(CircleShape)
                .background(DiscoveryColors.CatalogText),
            contentAlignment = Alignment.Center,
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

private const val DISABLED_ALPHA = 0.38f
