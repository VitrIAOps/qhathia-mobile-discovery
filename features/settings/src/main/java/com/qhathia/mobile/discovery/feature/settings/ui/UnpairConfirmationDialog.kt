package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.discovery.feature.settings.R

/**
 * Confirmation dialog for unpairing the merchant ("Desvincular comercio"). Confirming invokes the
 * stubbed unpair operation; the screen then surfaces that server-side unpairing is not available yet.
 */
@Composable
fun UnpairConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.pairing_unpair_title)) },
        text = { Text(text = stringResource(R.string.pairing_unpair_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.pairing_unpair_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.pairing_unpair_cancel))
            }
        },
    )
}
