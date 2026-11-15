package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing

/**
 * What happened to the cart after an add, shown by [CartFeedbackHost]: a confirmation that stays a few
 * seconds with "Ver carrito", or the 20-product limit, which stays until "Entendido" (UX-X-07).
 * Replaces the system Toasts the three screens used.
 */
@Stable
class CartFeedbackState {
    internal val host = SnackbarHostState()

    /** Shows "Agregaste…" for about 4 s; returns `true` when the customer tapped [actionLabel]. */
    suspend fun showAdded(message: String, actionLabel: String): Boolean {
        host.currentSnackbarData?.dismiss()
        return host.showSnackbar(message, actionLabel, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed
    }

    /** Shows the basket limit until the customer taps [actionLabel]. */
    suspend fun showLimit(message: String, actionLabel: String) {
        host.currentSnackbarData?.dismiss()
        host.showSnackbar(message, actionLabel, duration = SnackbarDuration.Indefinite)
    }
}

/**
 * Bottom-center host for [CartFeedbackState]: dark blue panel, white text (15.5:1), read politely by
 * screen readers. Place it over the screen content, above the bottom search bar.
 */
@Composable
fun CartFeedbackHost(state: CartFeedbackState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = state.host, modifier = modifier) { data ->
        Snackbar(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .padding(horizontal = Spacing.lg)
                .semantics { liveRegion = LiveRegionMode.Polite },
            shape = RoundedCornerShape(12.dp),
            containerColor = DiscoveryColors.CatalogText,
            contentColor = Color.White,
            action = data.visuals.actionLabel?.let { label ->
                {
                    TextButton(onClick = { data.performAction() }) {
                        Text(text = label, style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            },
        ) {
            Text(text = data.visuals.message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(name = "CartFeedbackHost — added", showBackground = true, widthDp = 800, heightDp = 160)
@Composable
private fun CartFeedbackHostPreview() {
    DiscoveryTheme(darkTheme = false) {
        Snackbar(
            containerColor = DiscoveryColors.CatalogText,
            contentColor = Color.White,
            action = { TextButton(onClick = {}) { Text("Ver carrito", color = Color.White) } },
        ) { Text("Agregaste «Taladro percutor 650W 13mm» al carrito") }
    }
}
