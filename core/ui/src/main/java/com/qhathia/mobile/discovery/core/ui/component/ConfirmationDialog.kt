package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme

/**
 * A two-action confirmation [AlertDialog]. Replaces the per-feature close-confirmation dialogs
 * ("¿Cerrar…?") and the unpair confirmation, which were each re-implemented in catalog,
 * product-detail, product-search and settings. Domain-free — the caller passes localized copy and
 * the confirm / dismiss handlers.
 *
 * @param confirmLabel label for the confirming action (e.g. "Sí, cerrar").
 * @param dismissLabel label for the dismissing action (e.g. "Cancelar").
 * @param containerColor optional dialog surface color; defaults to the Material dialog container.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    containerColor: Color? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        containerColor = containerColor ?: AlertDialogDefaults.containerColor,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissLabel)
            }
        },
    )
}

@Preview(name = "ConfirmationDialog — light")
@Composable
private fun ConfirmationDialogLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ConfirmationDialog(
            title = "¿Cerrar la búsqueda actual?",
            message = "Se cerrará la búsqueda y volverás al inicio.",
            confirmLabel = "Sí, cerrar",
            dismissLabel = "Cancelar",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "ConfirmationDialog — dark")
@Composable
private fun ConfirmationDialogDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ConfirmationDialog(
            title = "¿Cerrar la búsqueda actual?",
            message = "Se cerrará la búsqueda y volverás al inicio.",
            confirmLabel = "Sí, cerrar",
            dismissLabel = "Cancelar",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
