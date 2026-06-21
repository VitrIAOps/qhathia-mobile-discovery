package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme

/**
 * Inline validation / failure message rendered in the design-system error color. Replaces the
 * `bodySmall` + `colorScheme.error` `Text` that was duplicated across the cart and settings dialogs.
 * Domain-free — the caller passes the already-resolved, localized [text].
 *
 * @param text the localized error message.
 * @param textAlign optional alignment; left-aligned by default, pass [TextAlign.Center] for centered dialogs.
 */
@Composable
fun InlineErrorText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        textAlign = textAlign,
        modifier = modifier,
    )
}

@Preview(name = "InlineErrorText — light", showBackground = true)
@Composable
private fun InlineErrorTextLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        InlineErrorText(text = "No se pudo conectar a la red")
    }
}

@Preview(name = "InlineErrorText — dark", showBackground = true, backgroundColor = 0xFF0D1F38)
@Composable
private fun InlineErrorTextDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        InlineErrorText(text = "No se pudo conectar a la red")
    }
}
