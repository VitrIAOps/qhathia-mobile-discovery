package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme

/**
 * Feedback while voice search records: the animated "Escuchando…" [ProcessingMessage] until the first
 * words are recognized, then the live [transcript] in its place (no dots — it is not processing copy).
 *
 * The transcript is capped at about three lines and scrolls to its end as it grows, so the most recent
 * words stay visible. It is announced politely to accessibility services as it changes.
 *
 * @param transcript the live text recognized so far; blank while nothing has been recognized.
 * @param listeningText the listening message without trailing dots.
 */
@Composable
fun ListeningMessage(
    transcript: String,
    listeningText: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    AnimatedContent(
        targetState = transcript.isBlank(),
        transitionSpec = { fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS)) },
        contentAlignment = Alignment.Center,
        modifier = modifier,
        label = "ListeningMessage",
    ) { waitingForWords ->
        if (waitingForWords) {
            ProcessingMessage(baseText = listeningText, style = style, color = color)
        } else {
            LiveTranscript(transcript = transcript, style = style, color = color)
        }
    }
}

@Composable
private fun LiveTranscript(transcript: String, style: TextStyle, color: Color) {
    val scrollState = rememberScrollState()
    LaunchedEffect(transcript) { scrollState.animateScrollTo(scrollState.maxValue) }
    Box(
        modifier = Modifier
            .heightIn(max = maxHeightFor(style))
            .verticalScroll(scrollState),
    ) {
        Text(
            text = transcript,
            style = style,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** Height of [MAX_VISIBLE_LINES] lines of [style], falling back to the font size when line height is unset. */
@Composable
private fun maxHeightFor(style: TextStyle): Dp {
    val lineHeight = if (style.lineHeight.isSpecified) style.lineHeight else style.fontSize * FALLBACK_LINE_HEIGHT
    return with(LocalDensity.current) { lineHeight.toDp() * MAX_VISIBLE_LINES }
}

private const val FADE_MS = 200
private const val MAX_VISIBLE_LINES = 3
private const val FALLBACK_LINE_HEIGHT = 1.3f

private const val PREVIEW_LONG_TRANSCRIPT =
    "Estoy buscando un taladro inalámbrico de la marca Bosch que tenga dos baterías, un maletín " +
        "y que sirva para perforar concreto y madera sin problema"

@Preview(name = "ListeningMessage — waiting, light", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun ListeningMessageWaitingLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ListeningMessage(transcript = "", listeningText = "Escuchando")
    }
}

@Preview(name = "ListeningMessage — waiting, dark", showBackground = true, backgroundColor = 0xFF0D1F38)
@Composable
private fun ListeningMessageWaitingDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ListeningMessage(transcript = "", listeningText = "Escuchando")
    }
}

@Preview(name = "ListeningMessage — short transcript, light", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun ListeningMessageShortLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ListeningMessage(transcript = "Taladro de la marca", listeningText = "Escuchando")
    }
}

@Preview(name = "ListeningMessage — long transcript, dark", showBackground = true, backgroundColor = 0xFF0D1F38)
@Composable
private fun ListeningMessageLongDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ListeningMessage(transcript = PREVIEW_LONG_TRANSCRIPT, listeningText = "Escuchando")
    }
}
