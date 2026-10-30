package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing

/**
 * A full-width filled action button with an optional inline loading spinner and an optional trailing
 * icon. Replaces the `SubmitButton` (Wi-Fi connect) and the `FilledActionButton` (cart actions) that
 * were each re-implemented per feature. While [isLoading] is true the button shows a spinner and is
 * not clickable. Domain-free — the caller passes the localized [text] and handles [onClick].
 *
 * @param containerColor background color; defaults to the primary `Blue700` fill.
 * @param contentColor label / icon / spinner color; defaults to white.
 * @param leadingIconRes optional icon drawn after the label (e.g. the WhatsApp glyph on "Compartir").
 * @param fillMaxWidth `false` to size the button to its label (e.g. a "Reintentar" under a message).
 * @param iconContentDescription when set, read as the button's label instead of [text] (e.g.
 *   "Compartir por WhatsApp"), so the icon's meaning is not lost.
 */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = DiscoveryColors.Blue700,
    contentColor: Color = Color.White,
    leadingIconRes: Int? = null,
    iconContentDescription: String? = null,
    fillMaxWidth: Boolean = true,
) {
    Row(
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .labeled(iconContentDescription ?: text)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = contentColor, modifier = Modifier.size(20.dp))
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = contentColor)
            if (leadingIconRes != null) {
                Spacer(modifier = Modifier.width(Spacing.sm))
                Icon(
                    painter = painterResource(leadingIconRes),
                    contentDescription = null, // the label is on the button itself
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Preview(name = "PrimaryActionButton — light", showBackground = true, widthDp = 320)
@Composable
private fun PrimaryActionButtonLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        PrimaryActionButton(text = "Conectar", onClick = {})
    }
}

@Preview(name = "PrimaryActionButton — loading (dark)", showBackground = true, widthDp = 320, backgroundColor = 0xFF0D1F38)
@Composable
private fun PrimaryActionButtonLoadingPreview() {
    DiscoveryTheme(darkTheme = true) {
        PrimaryActionButton(text = "Conectar", onClick = {}, isLoading = true)
    }
}
