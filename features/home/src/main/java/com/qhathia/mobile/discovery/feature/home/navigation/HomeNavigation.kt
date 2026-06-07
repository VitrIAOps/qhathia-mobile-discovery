package com.qhathia.mobile.discovery.feature.home.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.feature.home.ui.HomeScreen

/** Stable route for the Discovery Home destination. */
const val HOME_ROUTE: String = "home"

/**
 * Registers the Home destination in a [NavGraphBuilder]. Navigation targets are hoisted as
 * callbacks so the destination screens are wired by the app. A typed or voiced query is handed off to
 * the product-search flow via [onNavigateToProductSearch] along with its [SearchSource].
 */
fun NavGraphBuilder.homeScreen(
    onNavigateToCatalog: () -> Unit,
    onNavigateToProductSearch: (String, SearchSource) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    composable(route = HOME_ROUTE) {
        HomeScreen(
            onNavigateToCatalog = onNavigateToCatalog,
            onNavigateToProductSearch = onNavigateToProductSearch,
            onNavigateToSettings = onNavigateToSettings,
            modifier = modifier,
        )
    }
}
