package com.qhathia.mobile.discovery.feature.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.discovery.core.ui.component.PrimaryActionButton
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.onboarding.R
import com.qhathia.mobile.discovery.feature.onboarding.model.WelcomeUiEffect
import com.qhathia.mobile.discovery.feature.onboarding.model.WelcomeUiEvent

/**
 * Stateful Welcome screen — the first-run entry point, and where an unlinked tablet lands. Reads the
 * unlink notice from [WelcomeViewModel] and forwards its navigation effect to [onContinue] (hoisted
 * to the onboarding graph). Kept separate from [WelcomeContent] so the layout stays previewable.
 */
@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                WelcomeUiEffect.NavigateToInitialConfiguration -> onContinue()
            }
        }
    }

    WelcomeContent(
        showUnlinkedNotice = uiState.showUnlinkedNotice,
        onContinue = { viewModel.onEvent(WelcomeUiEvent.Continue) },
        modifier = modifier,
    )
}

/**
 * Stateless Welcome UI: an optional unlink notice, a centered bienvenida title, the Qhathia application
 * name, a short supporting message, and the "Continuar" action that advances to the initial
 * configuration screen.
 */
@Composable
fun WelcomeContent(
    showUnlinkedNotice: Boolean,
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
            if (showUnlinkedNotice) UnlinkedNotice()
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

/** Tells the merchant the tablet was unlinked from the store; read out once by accessibility services. */
@Composable
private fun UnlinkedNotice() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text = stringResource(R.string.onboarding_unlinked_notice),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
        )
    }
}

@Preview(name = "Welcome — light", showBackground = true, widthDp = 960, heightDp = 800)
@Composable
private fun WelcomeContentLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        WelcomeContent(showUnlinkedNotice = false, onContinue = {})
    }
}

@Preview(name = "Welcome — dark", showBackground = true, widthDp = 960, heightDp = 800)
@Composable
private fun WelcomeContentDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        WelcomeContent(showUnlinkedNotice = false, onContinue = {})
    }
}

@Preview(name = "Welcome unlinked — light", showBackground = true, widthDp = 960, heightDp = 800)
@Composable
private fun WelcomeContentUnlinkedLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        WelcomeContent(showUnlinkedNotice = true, onContinue = {})
    }
}

@Preview(name = "Welcome unlinked — dark", showBackground = true, widthDp = 960, heightDp = 800)
@Composable
private fun WelcomeContentUnlinkedDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        WelcomeContent(showUnlinkedNotice = true, onContinue = {})
    }
}
