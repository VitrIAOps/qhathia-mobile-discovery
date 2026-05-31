package com.qhathia.mobile.discovery.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.qhathia.mobile.discovery.core.ui.tokens.DiscoveryShapes

private val LightColors = lightColorScheme(
    primary = DiscoveryColors.MicGradientStart,
    onPrimary = Color.White,
    secondary = DiscoveryColors.Blue600,
    onSecondary = Color.White,
    background = DiscoveryColors.Neutral50,
    onBackground = DiscoveryColors.Blue800,
    surface = DiscoveryColors.Neutral50,
    onSurface = DiscoveryColors.Blue800,
    surfaceVariant = Color.White,
    onSurfaceVariant = DiscoveryColors.Neutral500,
    outline = DiscoveryColors.SearchBorder,
)

private val DarkColors = darkColorScheme(
    primary = DiscoveryColors.MicGradientStart,
    onPrimary = Color.White,
    secondary = DiscoveryColors.MicGradientEnd,
    onSecondary = DiscoveryColors.Blue800,
    background = DiscoveryColors.DarkBackground,
    onBackground = DiscoveryColors.DarkOnSurface,
    surface = DiscoveryColors.DarkSurface,
    onSurface = DiscoveryColors.DarkOnSurface,
    surfaceVariant = DiscoveryColors.DarkSurface,
    onSurfaceVariant = DiscoveryColors.DarkOnSurfaceVariant,
    outline = DiscoveryColors.DarkOutline,
)

/**
 * Root theme for the Discovery app, built from the Qhathia design system. Brand-consistent by
 * design — dynamic color is intentionally not used so the kiosk always renders the Qhathia palette.
 * The DS ships a light scheme; [DarkColors] is a derived dark variant for dark-mode support.
 */
@Composable
fun DiscoveryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DiscoveryTypography,
        shapes = DiscoveryShapes,
        content = content,
    )
}
