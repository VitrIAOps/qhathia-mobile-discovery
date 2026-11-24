package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.qhathia.mobile.discovery.core.ui.R
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.voice.ui.VoiceMicButton
import com.qhathia.mobile.discovery.core.voice.ui.VoiceMicColors

/**
 * Discovery's microphone button: [VoiceMicButton] with the brand colors and icon.
 *
 * @param contentDescription localized description for assistive technologies (required, non-null).
 * @param onClick invoked when the button is tapped.
 * @param compact renders the smaller variant (~104dp container) used in the chat-area bottom bar; the
 *   default is the large hero size used on the Home and listening screens.
 *
 * Renders nothing when [LocalVoiceAssistantEnabled] is `false` (voice assistant turned off remotely).
 */
@Composable
fun MicButton(
    isListening: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    // Voice assistant turned off from the staff app: no mic, so voice search cannot start.
    if (!LocalVoiceAssistantEnabled.current) return

    VoiceMicButton(
        isListening = isListening,
        onClick = onClick,
        colors = VoiceMicColors(
            glow = DiscoveryColors.MicGlow,
            gradientStart = DiscoveryColors.MicGradientStart,
            gradientEnd = DiscoveryColors.MicGradientEnd,
        ),
        icon = painterResource(R.drawable.ic_mic),
        modifier = modifier,
        // The label is on the disc itself (it changes with the listening state).
        discModifier = Modifier.labeled(contentDescription),
        compact = compact,
    )
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
