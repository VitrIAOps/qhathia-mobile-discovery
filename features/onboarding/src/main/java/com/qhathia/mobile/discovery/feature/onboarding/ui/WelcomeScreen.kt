package com.qhathia.mobile.discovery.feature.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.discovery.core.ui.component.PrimaryActionButton
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.onboarding.R

/**
 * Stateful Welcome screen — the first-run entry point. Holds no state of its own; the single
 * navigation intent ([onContinue]) is hoisted to the caller (the onboarding graph). Kept separate from
 * [WelcomeContent] so the layout stays trivially previewable and testable.
 */
@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WelcomeContent(onContinue = onContinue, modifier = modifier)
}

/**
 * Stateless Welcome UI: a centered bienvenida title, the Qhathia application name, a short supporting
 * message, and the "Continuar" action that advances to the initial configuration screen.
 */
@Composable
fun WelcomeContent(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.titleMedium,
                color = DiscoveryColors.TextMuted,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.onboarding_app_name),
                style = MaterialTheme.typography.displaySmall,
                color = DiscoveryColors.Blue700,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.onboarding_welcome_message),
                style = MaterialTheme.typography.bodyLarge,
                color = DiscoveryColors.TextMuted,
                textAlign = TextAlign.Center,
            )
            PrimaryActionButton(
                text = stringResource(R.string.onboarding_continue),
                onClick = onContinue,
                modifier = Modifier.padding(top = Spacing.md),
            )
        }
    }
}

@Preview(name = "Welcome — light", showBackground = true, widthDp = 960, heightDp = 800)
@Composable
private fun WelcomeContentLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        WelcomeContent(onContinue = {})
    }
}

@Preview(name = "Welcome — dark", showBackground = true, widthDp = 960, heightDp = 800)
@Composable
private fun WelcomeContentDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        WelcomeContent(onContinue = {})
    }
}
