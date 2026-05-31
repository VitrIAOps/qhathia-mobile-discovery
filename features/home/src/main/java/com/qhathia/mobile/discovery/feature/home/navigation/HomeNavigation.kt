package com.qhathia.mobile.discovery.feature.home.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.discovery.feature.home.ui.HomeScreen

/** Stable route for the Discovery Home destination. */
const val HOME_ROUTE: String = "home"

/**
 * Registers the Home destination in a [NavGraphBuilder]. Navigation targets are hoisted as
 * callbacks so the destination screens (out of scope here) can be wired by the app.
 */
fun NavGraphBuilder.homeScreen(
    onNavigateToCatalog: () -> Unit,
    onNavigateToResults: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    composable(route = HOME_ROUTE) {
        HomeScreen(
            onNavigateToCatalog = onNavigateToCatalog,
            onNavigateToResults = onNavigateToResults,
            onNavigateToSettings = onNavigateToSettings,
            modifier = modifier,
        )
    }
}
