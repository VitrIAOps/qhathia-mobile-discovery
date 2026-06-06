package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animated voice waveform — a row of rounded vertical bars whose heights oscillate continuously,
 * recreating the "listening" equalizer from the Qhathia design-system animation
 * (listening-animation.gif) natively in Compose.
 *
 * The motion is decorative (a fixed sinusoidal loop), not reactive to real microphone amplitude.
 * Each bar follows the same sine wave with a per-bar phase offset, producing the travelling
 * equalizer effect. Stateless; the listening lifecycle is owned by the caller.
 *
 * Note: this is a runtime animation; it plays on device / in interactive preview, not in a static
 * `@Preview` snapshot.
 *
 * @param barColor color of the bars (defaults to white for use over the orange mic disc).
 * @param barCount number of bars to draw.
 */
@Composable
fun VoiceWaveform(
    modifier: Modifier = Modifier,
    barColor: Color = Color.White,
    barCount: Int = 6,
) {
    val transition = rememberInfiniteTransition(label = "voiceWaveform")
    // A single 0→2π phase sweep; each bar samples the sine at this phase plus its own offset.
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "voiceWaveformPhase",
    )

    Canvas(modifier = modifier) {
        // Bars share the width evenly; the gap is a fraction of each slot so bars stay chunky.
        val slot = size.width / barCount
        val barWidth = slot * 0.55f
        val corner = CornerRadius(barWidth / 2f)
        val minFraction = 0.30f
        val maxFraction = 1f

        for (i in 0 until barCount) {
            // Phase offset spreads the bars across the wave for the travelling-equalizer look.
            val offset = (i.toFloat() / barCount) * 2f * PI.toFloat()
            // sin → [-1,1] remapped to [0,1], then to the bar's height fraction.
            val wave = (sin(phase + offset) + 1f) / 2f
            val heightFraction = lerp(minFraction, maxFraction, wave)
            val barHeight = size.height * heightFraction

            val left = i * slot + (slot - barWidth) / 2f
            val top = (size.height - barHeight) / 2f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = corner,
            )
        }
    }
}

@Preview(name = "VoiceWaveform — light", showBackground = true, backgroundColor = 0xFFFF6A00)
@Composable
private fun VoiceWaveformPreview() {
    DiscoveryTheme(darkTheme = false) {
        VoiceWaveform(modifier = Modifier.size(68.dp))
    }
}
