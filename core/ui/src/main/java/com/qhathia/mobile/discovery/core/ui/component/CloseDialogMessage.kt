package com.qhathia.mobile.discovery.core.ui.component

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.discovery.core.ui.R

/**
 * Message of a screen's close-confirmation dialog. Confirming the close ends the customer session, so
 * with products in the basket the message warns that the cart will be emptied and says how many
 * products it has (same count as the cart badge). With an empty basket it is the screen's plain
 * [emptyCartMessage].
 */
@Composable
fun closeDialogMessage(cartItemCount: Int, @StringRes emptyCartMessage: Int): String =
    if (cartItemCount > 0) {
        pluralStringResource(R.plurals.discovery_close_dialog_message_with_cart, cartItemCount, cartItemCount)
    } else {
        stringResource(emptyCartMessage)
    }
