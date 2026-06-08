package com.qhathia.mobile.discovery.feature.productsearch.navigation

import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.feature.productsearch.ui.ProductSearchScreen

/** Navigation argument keys read by `ProductSearchViewModel` via `SavedStateHandle`. */
const val ARG_QUERY: String = "query"
const val ARG_SOURCE: String = "source"

private const val PRODUCT_SEARCH_BASE = "product_search"

/** Route pattern registered in the NavHost. */
const val PRODUCT_SEARCH_ROUTE: String = "$PRODUCT_SEARCH_BASE/{$ARG_QUERY}/{$ARG_SOURCE}"

/** Builds a concrete route for [query] originating from [source]. */
fun productSearchRoute(query: String, source: SearchSource): String =
    "$PRODUCT_SEARCH_BASE/${Uri.encode(query.ifBlank { " " })}/${source.name}"

/**
 * Registers the product-search destination. Navigation targets are hoisted as callbacks so the app
 * wires Home re-entry.
 */
fun NavGraphBuilder.productSearchScreen(
    onNavigateHomeIdle: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    composable(
        route = PRODUCT_SEARCH_ROUTE,
        arguments = listOf(
            navArgument(ARG_QUERY) { type = NavType.StringType },
            navArgument(ARG_SOURCE) { type = NavType.StringType },
        ),
    ) {
        ProductSearchScreen(
            onNavigateHomeIdle = onNavigateHomeIdle,
            onNavigateToDetail = onNavigateToDetail,
            onOpenCart = onOpenCart,
            modifier = modifier,
        )
    }
}
