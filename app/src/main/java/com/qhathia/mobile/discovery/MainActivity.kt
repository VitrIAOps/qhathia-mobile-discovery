package com.qhathia.mobile.discovery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.qhathia.mobile.discovery.core.domain.CartConstants
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.feature.cart.ui.CartDialog
import com.qhathia.mobile.discovery.feature.catalog.navigation.catalogRoute
import com.qhathia.mobile.discovery.feature.catalog.navigation.catalogScreen
import com.qhathia.mobile.discovery.feature.home.navigation.HOME_ROUTE
import com.qhathia.mobile.discovery.feature.home.navigation.homeScreen
import com.qhathia.mobile.discovery.feature.productdetail.navigation.productDetailRoute
import com.qhathia.mobile.discovery.feature.productdetail.navigation.productDetailScreen
import com.qhathia.mobile.discovery.feature.productsearch.navigation.productSearchRoute
import com.qhathia.mobile.discovery.feature.productsearch.navigation.productSearchScreen
import com.qhathia.mobile.discovery.feature.settings.navigation.NETWORK_SETTINGS_ROUTE
import com.qhathia.mobile.discovery.feature.settings.navigation.networkSettingsScreen
import com.qhathia.mobile.discovery.feature.settings.ui.SettingsPasswordGate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled at point of use */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Business rule: request microphone access when the app opens so voice search is ready.
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
        setContent {
            DiscoveryAppRoot()
        }
    }
}

@Composable
private fun DiscoveryAppRoot(
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    DiscoveryTheme {
        val navController = rememberNavController()

        // Cart is a dialog overlay hosted here (feature modules cannot depend on each other), opened
        // by the cart controls on the search and detail screens.
        var showCart by rememberSaveable { mutableStateOf(false) }

        // Settings PIN gate is an overlay hosted here so the PIN is requested before entering settings;
        // a correct PIN navigates straight to the Network settings screen.
        var showSettingsGate by rememberSaveable { mutableStateOf(false) }

        // Off-Home inactivity: any touch-down anywhere bumps the tick, restarting the 1-minute timer.
        var interactionTick by remember { mutableIntStateOf(0) }
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

        // Business rule: clear the cart after 1 minute of inactivity on any screen that isn't Home.
        LaunchedEffect(currentRoute, interactionTick) {
            if (currentRoute != null && currentRoute != HOME_ROUTE) {
                delay(CartConstants.INACTIVITY_TIMEOUT_SECONDS * 1_000L)
                mainViewModel.clearCart()
            }
        }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pointerInput(Unit) {
                        // Observe-only (Initial pass, no consume) so the timer resets without
                        // intercepting taps/scrolls meant for the screen below.
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.changes.any { it.changedToDownIgnoreConsumed() }) {
                                    interactionTick++
                                }
                            }
                        }
                    },
            ) {
                NavHost(
                    navController = navController,
                    startDestination = HOME_ROUTE,
                ) {
                    homeScreen(
                        onNavigateToCatalog = { navController.navigate(catalogRoute()) },
                        onNavigateToProductSearch = { query, source ->
                            navController.navigate(productSearchRoute(query, source))
                        },
                        onNavigateToSettings = { showSettingsGate = true },
                    )
                    catalogScreen(
                        onNavigateHomeIdle = { navController.popBackStack(HOME_ROUTE, inclusive = false) },
                        onNavigateToDetail = { productId -> navController.navigate(productDetailRoute(productId)) },
                        onNavigateToSearch = { query, source ->
                            navController.navigate(productSearchRoute(query, source))
                        },
                        onOpenCart = { showCart = true },
                    )
                    productSearchScreen(
                        onNavigateHomeIdle = { navController.popBackStack(HOME_ROUTE, inclusive = false) },
                        onNavigateToDetail = { productId -> navController.navigate(productDetailRoute(productId)) },
                        onOpenCart = { showCart = true },
                    )
                    productDetailScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateHomeIdle = { navController.popBackStack(HOME_ROUTE, inclusive = false) },
                        onNavigateToSearch = { query ->
                            navController.navigate(productSearchRoute(query, SearchSource.Text))
                        },
                        onNavigateToDetail = { productId -> navController.navigate(productDetailRoute(productId)) },
                        onOpenCart = { showCart = true },
                    )
                    networkSettingsScreen(
                        onNavigateBack = { navController.popBackStack(HOME_ROUTE, inclusive = false) },
                    )
                }
            }
        }

        if (showCart) {
            CartDialog(
                onNavigateToDetail = { productId ->
                    showCart = false
                    navController.navigate(productDetailRoute(productId))
                },
                onDismiss = { showCart = false },
            )
        }

        if (showSettingsGate) {
            SettingsPasswordGate(
                onUnlocked = {
                    showSettingsGate = false
                    navController.navigate(NETWORK_SETTINGS_ROUTE)
                },
                onDismiss = { showSettingsGate = false },
            )
        }
    }
}

