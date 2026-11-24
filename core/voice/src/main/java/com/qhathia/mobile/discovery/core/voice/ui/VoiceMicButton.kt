package com.qhathia.mobile.discovery.core.voice.ui

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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/** The colors of [VoiceMicButton], supplied by the host's design system. */
@Immutable
data class VoiceMicColors(
    val glow: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
)

/**
 * Large circular microphone button: a gradient disc that continuously emits soft expanding halo
 * rings. The disc stays fixed; the rings grow outward from its edge and fade.
 *
 * The ripple runs on its own (idle, no tap required); while [isListening] it pulses faster and the
 * [icon] switches to an animated [VoiceWaveform]. Stateless — the listening lifecycle is owned by the
 * caller.
 *
 * @param discModifier applied to the tappable disc, after its click: the place for its accessibility label.
 * @param compact renders the smaller variant (~104dp container); the default is the large hero size.
 */
@Composable
fun VoiceMicButton(
    isListening: Boolean,
    onClick: () -> Unit,
    colors: VoiceMicColors,
    icon: Painter,
    modifier: Modifier = Modifier,
    discModifier: Modifier = Modifier,
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
    val containerSize = circleSize + glowPadding

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
                            colors = listOf(colors.glow.copy(alpha = 0.8f), Color.Transparent),
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
                                        0.65f to colors.glow.copy(alpha = alpha),
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
                        colors = listOf(colors.gradientStart, colors.gradientEnd),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                )
                .clickable(onClick = onClick)
                .then(discModifier),
            contentAlignment = Alignment.Center,
        ) {
            if (isListening) {
                VoiceWaveform(barColor = Color.White, modifier = Modifier.size(waveformSize))
            } else {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}

@Preview(name = "VoiceMicButton — light", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Preview(name = "VoiceMicButton — dark", showBackground = true, backgroundColor = 0xFF0E1721, uiMode = 0x20)
@Composable
private fun VoiceMicButtonPreview() {
    VoiceMicButton(
        isListening = true,
        onClick = {},
        colors = VoiceMicColors(Color(0x66FF6A00), Color(0xFFFF6A00), Color(0xFFD93B00)),
        icon = ColorPainter(Color.White),
    )
}
