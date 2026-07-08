package com.qhathia.mobile.discovery.feature.pairing.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.discovery.core.ui.component.ConfirmationDialog
import com.qhathia.mobile.discovery.feature.pairing.R
import com.qhathia.mobile.discovery.core.ui.R as UiR

/**
 * Confirmation dialog for unpairing the merchant ("Desvincular comercio"). Confirming invokes the
 * stubbed unpair operation; the screen then surfaces that server-side unpairing is not available yet.
 * Built on the shared `core:ui` [ConfirmationDialog]; the cancel label comes from the shared kit.
 */
@Composable
fun UnpairConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(R.string.pairing_unpair_title),
        message = stringResource(R.string.pairing_unpair_message),
        confirmLabel = stringResource(R.string.pairing_unpair_confirm),
        dismissLabel = stringResource(UiR.string.discovery_dialog_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
