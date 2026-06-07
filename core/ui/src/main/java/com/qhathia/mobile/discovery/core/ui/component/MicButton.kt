package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.qhathia.mobile.discovery.core.ui.R
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme

/**
 * Large circular microphone button for voice search — an orange gradient disc that continuously
 * emits soft expanding halo rings ("ripple"), matching the Qhathia design-system animation
 * (button-animation.gif). The disc stays fixed; the rings grow outward from its edge and fade.
 *
 * The ripple runs on its own (idle, no tap required); while [isListening] it pulses faster and the
 * mic icon switches to an animated [VoiceWaveform]. Stateless — the listening lifecycle is owned by
 * the caller.
 *
 * Note: this is a runtime animation; it plays on device / in interactive preview, not in a static
 * `@Preview` snapshot.
 *
 * @param contentDescription localized description for assistive technologies (required, non-null).
 * @param onClick invoked when the button is tapped.
 * @param compact renders the smaller variant (~104dp container) used in the chat-area bottom bar; the
 *   default is the large hero size used on the Home and listening screens.
 */
@Composable
fun MicButton(
    isListening: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val idleDisc = if (compact) 58.dp else 150.dp
    val listeningDisc = if (compact) 80.dp else 190.dp
    val idleGlow = if (compact) 20.dp else 64.dp
    val listeningGlow = if (compact) 28.dp else 100.dp
    val iconSize = if (compact) 28.dp else 48.dp
    val waveformSize = if (compact) 36.dp else 68.dp

    // Listening makes the disc noticeably larger and the halos reach further.
    val circleSize by animateDpAsState(
        targetValue = if (isListening) listeningDisc else idleDisc,
        label = "micCircleSize",
    )
    val glowPadding by animateDpAsState(
        targetValue = if (isListening) listeningGlow else idleGlow,
        label = "micGlowPadding",
    )
    val containerSize = circleSize + glowPadding * 2

    val transition = rememberInfiniteTransition(label = "micPulse")
    // A single 0→1 ripple progress; a second ring is derived at +0.5 phase for continuity.
    val rippleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 1600 else 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "micRipple",
    )

    Box(modifier = modifier.size(containerSize), contentAlignment = Alignment.Center) {
        // Expanding halo rings + a soft base glow.
        Box(
            modifier = Modifier
                .size(containerSize)
                .drawBehind {
                    val maxRadius = size.minDimension / 2f
                    val discRadius = circleSize.toPx() / 2f
                    val peakAlpha = if (isListening) 0.62f else 0.45f

                    // Steady base glow hugging the disc.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(DiscoveryColors.MicGlow.copy(alpha = 0.8f), Color.Transparent),
                            center = center,
                            radius = discRadius * 1.4f,
                        ),
                    )

                    // Two staggered expanding rings (soft bands that grow and fade).
                    for (phase in floatArrayOf(rippleProgress, (rippleProgress + 0.5f) % 1f)) {
                        val ringRadius = lerp(discRadius, maxRadius, phase)
                        val alpha = (1f - phase) * peakAlpha
                        if (alpha > 0.01f) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Transparent,
                                        0.65f to DiscoveryColors.MicGlow.copy(alpha = alpha),
                                        1.0f to Color.Transparent,
                                    ),
                                    center = center,
                                    radius = ringRadius,
                                ),
                                radius = ringRadius,
                                center = center,
                            )
                        }
                    }
                },
        )
        // Gradient disc.
        Box(
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(DiscoveryColors.MicGradientStart, DiscoveryColors.MicGradientEnd),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (isListening) {
                val waveformDescription = contentDescription
                VoiceWaveform(
                    barColor = Color.White,
                    modifier = Modifier
                        .size(waveformSize)
                        .semantics { this.contentDescription = waveformDescription },
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}

@Preview(name = "MicButton idle — light", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun MicButtonIdlePreview() {
    DiscoveryTheme(darkTheme = false) {
        MicButton(isListening = false, contentDescription = "Search by voice", onClick = {})
    }
}

@Preview(name = "MicButton listening — light", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun MicButtonListeningPreview() {
    DiscoveryTheme(darkTheme = true) {
        MicButton(isListening = true, contentDescription = "Listening", onClick = {})
    }
}
