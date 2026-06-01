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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.feature.home.navigation.HOME_ROUTE
import com.qhathia.mobile.discovery.feature.home.navigation.homeScreen
import dagger.hilt.android.AndroidEntryPoint

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
private fun DiscoveryAppRoot() {
    DiscoveryTheme {
        val navController = rememberNavController()
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = HOME_ROUTE,
                modifier = Modifier.padding(innerPadding),
            ) {
                homeScreen(
                    onNavigateToCatalog = { navController.navigate(CATALOG_ROUTE) },
                    onNavigateToResults = { query ->
                        navController.navigate("$RESULTS_ROUTE/${query.ifBlank { " " }}")
                    },
                    onNavigateToSettings = { navController.navigate(SETTINGS_ROUTE) },
                )
                placeholderDestinations()
            }
        }
    }
}

// --- Placeholder destinations (catalog / results / settings screens are out of scope) ---

private const val CATALOG_ROUTE = "catalog"
private const val RESULTS_ROUTE = "results"
private const val SETTINGS_ROUTE = "settings"

private fun NavGraphBuilder.placeholderDestinations() {
    composable(CATALOG_ROUTE) { PlaceholderScreen(title = "Catalog") }
    composable("$RESULTS_ROUTE/{query}") { backStackEntry ->
        val query = backStackEntry.arguments?.getString("query").orEmpty().trim()
        PlaceholderScreen(title = "Results: $query")
    }
    composable(SETTINGS_ROUTE) { PlaceholderScreen(title = "Settings") }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title)
    }
}
