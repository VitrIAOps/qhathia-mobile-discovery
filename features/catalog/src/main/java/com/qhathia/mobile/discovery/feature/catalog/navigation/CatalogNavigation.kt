package com.qhathia.mobile.discovery.feature.catalog.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.feature.catalog.ui.CatalogScreen

/** Navigation argument key read by `CatalogViewModel` via `SavedStateHandle`. */
const val ARG_SOURCE: String = "source"

private const val CATALOG_BASE = "catalog"

/** Route pattern registered in the NavHost. */
const val CATALOG_ROUTE: String = "$CATALOG_BASE/{$ARG_SOURCE}"

/** Builds a concrete catalog route for an entry originating from [source] (defaults to a direct entry). */
fun catalogRoute(source: SearchSource = SearchSource.Text): String = "$CATALOG_BASE/${source.name}"

/**
 * Registers the catalog destination. Navigation targets are hoisted as callbacks so the app wires Home
 * re-entry, product detail, the search hand-off, and the cart overlay.
 */
fun NavGraphBuilder.catalogScreen(
    onNavigateHomeIdle: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSearch: (String, SearchSource) -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    composable(
        route = CATALOG_ROUTE,
        arguments = listOf(
            navArgument(ARG_SOURCE) {
                type = NavType.StringType
                defaultValue = SearchSource.Text.name
            },
        ),
    ) {
        CatalogScreen(
            onNavigateHomeIdle = onNavigateHomeIdle,
            onNavigateToDetail = onNavigateToDetail,
            onNavigateToSearch = onNavigateToSearch,
            onOpenCart = onOpenCart,
            modifier = modifier,
        )
    }
}
