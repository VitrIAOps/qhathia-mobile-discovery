package com.qhathia.mobile.discovery.feature.productdetail.ui

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.qhathia.mobile.discovery.core.ui.component.KioskImmersiveDialogEffect
import com.qhathia.mobile.discovery.core.ui.component.reportUserActivity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.qhathia.mobile.discovery.core.ui.component.CloseIconButton
import com.qhathia.mobile.discovery.core.ui.component.DiscoveryTextField
import com.qhathia.mobile.discovery.core.ui.component.PrimaryActionButton
import com.qhathia.mobile.discovery.core.ui.component.SubmissionErrorContent
import com.qhathia.mobile.discovery.core.ui.component.SubmissionSuccessContent
import com.qhathia.mobile.discovery.core.ui.component.SubmittingOverlay
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.productdetail.R
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiEvent
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiState.ShareSubmission
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiState.ShareUiState
import com.qhathia.mobile.discovery.core.ui.R as UiR

/**
 * The "Compartir por WhatsApp" dialog on the product-detail screen. Collects the consumer's name +
 * phone (validated exactly like the cart) and shares a quote PDF of the selected product, reusing the
 * cart's loading/success/error overlays from `core:ui`. Stateless — a pure function of [state].
 */
@Composable
fun ShareProductDialog(
    state: ShareUiState,
    onEvent: (ProductDetailUiEvent) -> Unit,
) {
    Dialog(
        onDismissRequest = { onEvent(ProductDetailUiEvent.ShareDismissRequest) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Kiosk: keep the system bars hidden over this dialog window and its keyboard.
        KioskImmersiveDialogEffect()
        // Mirror the cart: open WITHOUT popping the soft keyboard by parking the initial focus on an
        // invisible trap and keeping the IME hidden on entry.
        val view = LocalView.current
        val focusTrap = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            generateSequence(view.parent) { it.parent }
                .filterIsInstance<DialogWindowProvider>()
                .firstOrNull()
                ?.window
                ?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
            runCatching { focusTrap.requestFocus() }
        }

        // A dialog is its own window: report its touches to the inactivity timer.
        Box(modifier = Modifier.reportUserActivity(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(focusTrap)
                    .focusable(),
            )
            Box(modifier = Modifier.size(width = DIALOG_WIDTH, height = DIALOG_HEIGHT)) {
                ShareDialogContent(state = state, onEvent = onEvent, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun ShareDialogContent(
    state: ShareUiState,
    onEvent: (ProductDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val submission = state.submission) {
        is ShareSubmission.Success ->
            SubmissionSuccessContent(
                title = stringResource(R.string.pd_share_success_title),
                subtitle = stringResource(R.string.pd_share_success_subtitle),
                summary = submission.summary,
                footnote = stringResource(R.string.pd_share_success_check_phone),
                actionLabel = stringResource(R.string.pd_share_success_accept),
                onAction = { onEvent(ProductDetailUiEvent.AcceptShareSuccess) },
                quantityLabel = { pluralStringResource(R.plurals.pd_share_success_qty, it, it) },
                modifier = modifier,
            )

        is ShareSubmission.Error ->
            SubmissionErrorContent(
                title = stringResource(R.string.pd_share_error_title),
                message = stringResource(
                    if (submission.network) R.string.pd_share_error_network_message else R.string.pd_share_error_message,
                ),
                primaryLabel = stringResource(R.string.pd_share_error_understood),
                onPrimary = { onEvent(ProductDetailUiEvent.DismissShareError) },
                secondaryLabel = stringResource(R.string.pd_share_error_cancel),
                onSecondary = { onEvent(ProductDetailUiEvent.DismissShare) },
                modifier = modifier,
            )

        ShareSubmission.NotDelivered ->
            SubmissionErrorContent(
                title = stringResource(R.string.pd_share_not_delivered_title),
                message = stringResource(R.string.pd_share_not_delivered_message),
                primaryLabel = stringResource(R.string.pd_share_error_understood),
                onPrimary = { onEvent(ProductDetailUiEvent.DismissShareError) },
                secondaryLabel = stringResource(R.string.pd_share_error_cancel),
                onSecondary = { onEvent(ProductDetailUiEvent.DismissShare) },
                modifier = modifier,
            )

        is ShareSubmission.Unavailable ->
            SubmissionErrorContent(
                title = stringResource(R.string.pd_share_unavailable_title),
                message = submission.productName
                    ?.let { stringResource(R.string.pd_share_unavailable_message, it) }
                    ?: stringResource(R.string.pd_share_unavailable_message_generic),
                primaryLabel = stringResource(R.string.pd_share_error_understood),
                onPrimary = { onEvent(ProductDetailUiEvent.DismissShareError) },
                secondaryLabel = null,
                onSecondary = {},
                modifier = modifier,
            )

        is ShareSubmission.StockShortage ->
            SubmissionErrorContent(
                title = stringResource(R.string.pd_share_stock_shortage_title),
                message = pluralStringResource(
                    R.plurals.pd_share_stock_shortage_message,
                    submission.available,
                    submission.available,
                ),
                primaryLabel = stringResource(R.string.pd_share_error_understood),
                onPrimary = { onEvent(ProductDetailUiEvent.DismissShareError) },
                secondaryLabel = stringResource(R.string.pd_share_error_cancel),
                onSecondary = { onEvent(ProductDetailUiEvent.DismissShare) },
                modifier = modifier,
            )

        ShareSubmission.Idle, ShareSubmission.Submitting ->
            ShareForm(state = state, onEvent = onEvent, modifier = modifier)
    }
}

@Composable
private fun ShareForm(
    state: ShareUiState,
    onEvent: (ProductDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(
                        text = stringResource(R.string.pd_share_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    CloseIconButton(
                        onClick = { onEvent(ProductDetailUiEvent.DismissShare) },
                        contentDescription = stringResource(R.string.pd_share_close_cd),
                        size = 48.dp,
                        iconSize = 16.dp,
                    )
                }
                DiscoveryTextField(
                    value = state.name,
                    onValueChange = { onEvent(ProductDetailUiEvent.ShareNameChanged(it)) },
                    label = stringResource(R.string.pd_share_name_label),
                    placeholder = stringResource(R.string.pd_share_name_placeholder),
                    isError = state.showNameError,
                    errorText = if (state.showNameError) stringResource(R.string.pd_share_name_error) else null,
                    leadingIconRes = UiR.drawable.ic_user,
                    keyboardType = KeyboardType.Text,
                )
                DiscoveryTextField(
                    value = state.phone,
                    onValueChange = { onEvent(ProductDetailUiEvent.SharePhoneChanged(it)) },
                    label = stringResource(R.string.pd_share_phone_label),
                    placeholder = stringResource(R.string.pd_share_phone_placeholder),
                    isError = state.showPhoneError,
                    errorText = if (state.showPhoneError) stringResource(R.string.pd_share_phone_error) else null,
                    leadingIconRes = UiR.drawable.ic_phone,
                    keyboardType = KeyboardType.Phone,
                )
                PrimaryActionButton(
                    text = stringResource(R.string.pd_share_submit),
                    onClick = { onEvent(ProductDetailUiEvent.ConfirmShare) },
                    enabled = state.isShareEnabled,
                    containerColor = if (state.isShareEnabled) DiscoveryColors.Green500 else DiscoveryColors.Divider,
                    contentColor = if (state.isShareEnabled) Color.White else DiscoveryColors.TextDisabled,
                    leadingIconRes = UiR.drawable.ic_whatsapp,
                    iconContentDescription = stringResource(R.string.pd_share_whatsapp_cd),
                    modifier = Modifier.widthIn(max = PANEL_MAX_WIDTH),
                )
                PrimaryActionButton(
                    text = stringResource(R.string.pd_share_cancel),
                    onClick = { onEvent(ProductDetailUiEvent.DismissShare) },
                    containerColor = Color.Transparent,
                    contentColor = DiscoveryColors.CatalogText,
                    modifier = Modifier.widthIn(max = PANEL_MAX_WIDTH),
                )
            }
        }
        if (state.isSubmitting) {
            SubmittingOverlay(contentDescription = stringResource(R.string.pd_share_submitting_cd))
        }
    }
}

private val DIALOG_WIDTH = 560.dp
private val DIALOG_HEIGHT = 620.dp
private val PANEL_MAX_WIDTH = 480.dp

// --- Previews ---

@androidx.compose.ui.tooling.preview.Preview(name = "Share — form (light)", showBackground = true, widthDp = 640, heightDp = 720)
@Composable
private fun ShareFormLightPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = false) {
        ShareDialogContent(
            state = ShareUiState(visible = true, name = "Ana Pérez", phone = "987654321", isShareEnabled = true),
            onEvent = {},
            modifier = Modifier.size(width = DIALOG_WIDTH, height = DIALOG_HEIGHT),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Share — success (dark)", showBackground = true, widthDp = 640, heightDp = 720)
@Composable
private fun ShareSuccessDarkPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = true) {
        ShareDialogContent(
            state = ShareUiState(
                visible = true,
                submission = ShareSubmission.Success(
                    kotlinx.collections.immutable.persistentListOf(
                        com.qhathia.mobile.discovery.core.ui.component.SubmissionLine("", "Taladro Percutor Bosch GSB 13 RE", 2),
                    ),
                ),
            ),
            onEvent = {},
            modifier = Modifier.size(width = DIALOG_WIDTH, height = DIALOG_HEIGHT),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Share — no network (dark)", showBackground = true, widthDp = 640, heightDp = 720)
@Composable
private fun ShareNetworkErrorDarkPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = true) {
        ShareDialogContent(
            state = ShareUiState(visible = true, submission = ShareSubmission.Error(network = true)),
            onEvent = {},
            modifier = Modifier.size(width = DIALOG_WIDTH, height = DIALOG_HEIGHT),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Share — error (light)", showBackground = true, widthDp = 640, heightDp = 720)
@Composable
private fun ShareErrorLightPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = false) {
        ShareDialogContent(
            state = ShareUiState(visible = true, submission = ShareSubmission.Error()),
            onEvent = {},
            modifier = Modifier.size(width = DIALOG_WIDTH, height = DIALOG_HEIGHT),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Share — not delivered (light)", showBackground = true, widthDp = 640, heightDp = 720)
@Composable
private fun ShareNotDeliveredLightPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = false) {
        ShareDialogContent(
            state = ShareUiState(visible = true, submission = ShareSubmission.NotDelivered),
            onEvent = {},
            modifier = Modifier.size(width = DIALOG_WIDTH, height = DIALOG_HEIGHT),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Share — not delivered (dark)", showBackground = true, widthDp = 640, heightDp = 720)
@Composable
private fun ShareNotDeliveredDarkPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = true) {
        ShareDialogContent(
            state = ShareUiState(visible = true, submission = ShareSubmission.NotDelivered),
            onEvent = {},
            modifier = Modifier.size(width = DIALOG_WIDTH, height = DIALOG_HEIGHT),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Share — submitting (dark)", showBackground = true, widthDp = 640, heightDp = 720)
@Composable
private fun ShareSubmittingDarkPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = true) {
        ShareDialogContent(
            state = ShareUiState(
                visible = true,
                name = "Ana Pérez",
                phone = "987654321",
                isShareEnabled = true,
                submission = ShareSubmission.Submitting,
            ),
            onEvent = {},
            modifier = Modifier.size(width = DIALOG_WIDTH, height = DIALOG_HEIGHT),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Share — unavailable (light)", showBackground = true, widthDp = 640, heightDp = 720)
@Composable
private fun ShareUnavailableLightPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = false) {
        ShareDialogContent(
            state = ShareUiState(visible = true, submission = ShareSubmission.Unavailable("Taladro Percutor Bosch GSB 13 RE")),
            onEvent = {},
            modifier = Modifier.size(width = DIALOG_WIDTH, height = DIALOG_HEIGHT),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Share — unavailable (dark)", showBackground = true, widthDp = 640, heightDp = 720)
@Composable
private fun ShareUnavailableDarkPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = true) {
        ShareDialogContent(
            state = ShareUiState(visible = true, submission = ShareSubmission.Unavailable(null)),
            onEvent = {},
            modifier = Modifier.size(width = DIALOG_WIDTH, height = DIALOG_HEIGHT),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Share — stock shortage (light)", showBackground = true, widthDp = 640, heightDp = 720)
@Composable
private fun ShareStockShortageLightPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = false) {
        ShareDialogContent(
            state = ShareUiState(visible = true, submission = ShareSubmission.StockShortage(2)),
            onEvent = {},
            modifier = Modifier.size(width = DIALOG_WIDTH, height = DIALOG_HEIGHT),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Share — stock shortage (dark)", showBackground = true, widthDp = 640, heightDp = 720)
@Composable
private fun ShareStockShortageDarkPreview() {
    com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme(darkTheme = true) {
        ShareDialogContent(
            state = ShareUiState(visible = true, submission = ShareSubmission.StockShortage(1)),
            onEvent = {},
            modifier = Modifier.size(width = DIALOG_WIDTH, height = DIALOG_HEIGHT),
        )
    }
}
