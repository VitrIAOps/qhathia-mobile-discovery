package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.R
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme

/**
 * A square icon button that dismisses/closes its surface, drawn with the shared `ic_close` glyph.
 * Replaces the bordered close "X" boxes that were hand-rolled in the cart and Wi-Fi dialogs (and the
 * borderless per-line remove control). Defaults match the dialog close button; override [size],
 * [iconSize], [cornerRadius], [bordered] and [tint] for the other variants.
 *
 * @param onClick invoked when tapped.
 * @param contentDescription localized accessibility label (e.g. "Cerrar carrito").
 */
@Composable
fun CloseIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 14.dp,
    cornerRadius: Dp = 10.dp,
    bordered: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(if (bordered) Modifier.border(1.dp, DiscoveryColors.Divider, shape) else Modifier)
            .clickable(onClick = onClick)
            .then(if (contentDescription != null) Modifier.labeled(contentDescription) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = null, // the label is on the button itself
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Preview(name = "CloseIconButton — light", showBackground = true)
@Composable
private fun CloseIconButtonLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        CloseIconButton(onClick = {}, contentDescription = "Cerrar")
    }
}

@Preview(name = "CloseIconButton — dark", showBackground = true, backgroundColor = 0xFF0D1F38)
@Composable
private fun CloseIconButtonDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        CloseIconButton(onClick = {}, contentDescription = "Cerrar")
    }
}
