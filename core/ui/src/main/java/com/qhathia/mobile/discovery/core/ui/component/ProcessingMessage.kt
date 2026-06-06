package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import kotlinx.coroutines.delay

/**
 * A processing/“thinking” label with recursively animated trailing dots: 0 → 1 → 2 → 3 → 0…
 * (e.g. "Escuchando…", "Organizando ideas…"). Three dots are always laid out (inactive ones are
 * transparent) so the centered label keeps a constant width and does not jump as the dots cycle.
 *
 * Use only for processing states — per the design system the animated dots must not appear on
 * non-processing copy.
 *
 * @param baseText the message without trailing dots (the dots are appended and animated here).
 */
@Composable
fun ProcessingMessage(
    baseText: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    val dotCount by produceState(initialValue = 0) {
        while (true) {
            delay(DOT_INTERVAL_MS)
            value = (value + 1) % (MAX_DOTS + 1)
        }
    }
    val text = buildAnnotatedString {
        append(baseText)
        repeat(MAX_DOTS) { index ->
            withStyle(SpanStyle(color = if (index < dotCount) color else Color.Transparent)) {
                append(".")
            }
        }
    }
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

private const val DOT_INTERVAL_MS = 400L
private const val MAX_DOTS = 3

@Preview(name = "ProcessingMessage — light", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun ProcessingMessageLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProcessingMessage(baseText = "Organizando ideas")
    }
}

@Preview(name = "ProcessingMessage — dark", showBackground = true, backgroundColor = 0xFF0D1F38)
@Composable
private fun ProcessingMessageDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProcessingMessage(baseText = "Escuchando")
    }
}
