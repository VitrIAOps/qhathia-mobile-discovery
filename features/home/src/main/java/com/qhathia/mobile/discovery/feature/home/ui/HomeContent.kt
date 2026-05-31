package com.qhathia.mobile.discovery.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.component.DiscoverySearchBar
import com.qhathia.mobile.discovery.core.ui.component.MicButton
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.home.R
import com.qhathia.mobile.discovery.feature.home.model.HomeUiEvent
import com.qhathia.mobile.discovery.feature.home.model.HomeUiState
import com.qhathia.mobile.discovery.feature.home.model.MicState

/**
 * Stateless Home UI — a pure function of [HomeUiState], built from the Qhathia design system to
 * match the Figma Home flow (node 286-9269).
 *
 * Layout: "TIENDA" + "Ir al catálogo" top bar, a centered hero (title + subtitle + mic + search),
 * a settings gear bottom-start, and a "powered by Qhathia" footer. The search bar, catalog link and
 * gear are hidden only while actively listening ([HomeUiState.areControlsVisible]).
 */
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val entranceAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        label = "homeEntrance",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = entranceAlpha },
    ) {
        TopBar(
            storeName = uiState.storeName,
            controlsVisible = uiState.areControlsVisible,
            onCatalogClick = { onEvent(HomeUiEvent.OpenCatalog) },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        HeroSection(
            uiState = uiState,
            onEvent = onEvent,
            modifier = Modifier.align(Alignment.Center),
        )

        AnimatedVisibility(
            visible = uiState.areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = Spacing.lg, bottom = Spacing.lg),
        ) {
            IconButton(onClick = { onEvent(HomeUiEvent.OpenSettings) }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.home_settings_cd),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        PoweredByFooter(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.xl),
        )
    }
}

@Composable
private fun TopBar(
    storeName: String?,
    controlsVisible: Boolean,
    onCatalogClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (storeName?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.home_store_name_placeholder)).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = DiscoveryColors.Blue800,
        )
        AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
            CatalogLink(onClick = onCatalogClick)
        }
    }
}

@Composable
private fun CatalogLink(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_catalog),
            style = MaterialTheme.typography.labelLarge,
            color = DiscoveryColors.CatalogText,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = DiscoveryColors.CatalogText,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun HeroSection(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val micState = uiState.micState
    Column(
        modifier = modifier.padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (micState is MicState.Listening) {
            MicButton(
                isListening = true,
                contentDescription = stringResource(R.string.home_mic_listening_cd),
                onClick = { onEvent(HomeUiEvent.CancelMic) },
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            ListeningLabel(
                baseText = stringResource(R.string.home_listening_label),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        } else {
            val isRetry = micState is MicState.NoAudioRetry
            val (title, subtitle) = headlineFor(micState)
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = DiscoveryColors.Blue600,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 576.dp),
            )
            Spacer(modifier = Modifier.height(Spacing.xl))
            MicButton(
                isListening = false,
                contentDescription = stringResource(
                    if (isRetry) R.string.home_mic_retry_cd else R.string.home_mic_cd,
                ),
                onClick = {
                    onEvent(if (isRetry) HomeUiEvent.RetryMic else HomeUiEvent.ActivateMic)
                },
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            DiscoverySearchBar(
                query = uiState.query,
                onQueryChange = { onEvent(HomeUiEvent.QueryChanged(it)) },
                onSearch = { onEvent(HomeUiEvent.SubmitSearch) },
                placeholder = stringResource(R.string.home_search_placeholder),
                searchIconContentDescription = stringResource(R.string.home_search_icon_cd),
                modifier = Modifier.width(600.dp),
            )
        }
    }
}

/** Resolves the hero (title, subtitle) pair for the idle and no-audio-retry states. */
@Composable
private fun headlineFor(micState: MicState): Pair<String, String> = when (micState) {
    is MicState.NoAudioRetry -> {
        val titles = stringArrayResource(R.array.home_retry_titles)
        val subtitles = stringArrayResource(R.array.home_retry_subtitles)
        val i = micState.promptIndex % titles.size
        titles[i] to subtitles[i % subtitles.size]
    }
    else -> stringResource(R.string.home_title) to stringResource(R.string.home_subtitle)
}

/**
 * "Escuchando" with recursively animated trailing dots: 0 → 1 → 2 → 3 → 0… Three dots are always
 * laid out (inactive ones are transparent) so the centered label keeps a constant width and does
 * not jump as the dots cycle.
 */
@Composable
private fun ListeningLabel(
    baseText: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val dotCount by produceState(initialValue = 0) {
        while (true) {
            kotlinx.coroutines.delay(400L)
            value = (value + 1) % 4
        }
    }
    val text = buildAnnotatedString {
        append(baseText)
        repeat(3) { index ->
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

@Composable
private fun PoweredByFooter(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.home_powered_by),
            style = MaterialTheme.typography.bodyMedium,
            color = DiscoveryColors.Footer,
        )
        Text(
            text = stringResource(R.string.home_brand),
            style = MaterialTheme.typography.headlineLarge,
            color = DiscoveryColors.Footer,
        )
    }
}

// --- Previews (landscape kiosk) ---

@Preview(name = "Home idle — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun HomeContentIdleLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        HomeContent(
            uiState = HomeUiState(storeName = "Joyería María", micState = MicState.Idle),
            onEvent = {},
        )
    }
}

@Preview(name = "Home listening — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun HomeContentListeningPreview() {
    DiscoveryTheme(darkTheme = false) {
        HomeContent(
            uiState = HomeUiState(storeName = "Joyería María", micState = MicState.Listening(30)),
            onEvent = {},
        )
    }
}

@Preview(name = "Home retry — light", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun HomeContentRetryPreview() {
    DiscoveryTheme(darkTheme = false) {
        HomeContent(
            uiState = HomeUiState(storeName = "Joyería María", micState = MicState.NoAudioRetry(0)),
            onEvent = {},
        )
    }
}

@Preview(name = "Home idle — dark", showBackground = true, widthDp = 1024, heightDp = 640)
@Composable
private fun HomeContentIdleDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        HomeContent(
            uiState = HomeUiState(storeName = "Joyería María", micState = MicState.Idle),
            onEvent = {},
        )
    }
}
