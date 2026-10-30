package com.qhathia.mobile.discovery.feature.productdetail.navigation

import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.feature.productdetail.ui.ProductDetailScreen

/** Navigation argument key read by `ProductDetailViewModel` via `SavedStateHandle`. */
const val ARG_PRODUCT_ID: String = "productId"

private const val PRODUCT_DETAIL_BASE = "product_detail"

/** Route pattern registered in the NavHost. */
const val PRODUCT_DETAIL_ROUTE: String = "$PRODUCT_DETAIL_BASE/{$ARG_PRODUCT_ID}"

/** Builds a concrete route for [productId]. */
fun productDetailRoute(productId: String): String =
    "$PRODUCT_DETAIL_BASE/${Uri.encode(productId)}"

/**
 * Registers the product-detail destination. Navigation targets are hoisted as callbacks so the app
 * wires Volver (back to results) and the confirmed new-search (Home idle) re-entry.
 */
fun NavGraphBuilder.productDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateHomeIdle: () -> Unit,
    onNavigateToSearch: (String, SearchSource) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    composable(
        route = PRODUCT_DETAIL_ROUTE,
        arguments = listOf(
            navArgument(ARG_PRODUCT_ID) { type = NavType.StringType },
        ),
    ) {
        ProductDetailScreen(
            onNavigateBack = onNavigateBack,
            onNavigateHomeIdle = onNavigateHomeIdle,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToDetail = onNavigateToDetail,
            onOpenCart = onOpenCart,
            modifier = modifier,
        )
    }
}
