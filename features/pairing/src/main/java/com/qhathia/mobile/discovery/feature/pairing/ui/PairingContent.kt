package com.qhathia.mobile.discovery.feature.pairing.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qhathia.mobile.discovery.core.domain.PairingConstants
import com.qhathia.mobile.discovery.core.domain.model.PairedDevice
import com.qhathia.mobile.discovery.core.ui.component.DiscoveryDivider
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.pairing.R
import com.qhathia.mobile.discovery.core.ui.R as UiR
import com.qhathia.mobile.discovery.feature.pairing.model.PairingError
import com.qhathia.mobile.discovery.feature.pairing.model.PairingPhase
import com.qhathia.mobile.discovery.feature.pairing.model.PairingUiEvent
import com.qhathia.mobile.discovery.feature.pairing.model.PairingUiState
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Stateless pairing (Vincular) UI — a pure function of [PairingUiState]. Renders the "Código generado"
 * layout (Figma 1055-14805) while waiting for approval and the "Comercio vinculado" layout
 * (Figma 1080-1587) once paired, plus loading/rejected/error states.
 *
 * Host-agnostic: [title] is the header title (TIENDA / QHATHIA) and [showBackButton] / [onBack] render
 * the optional back affordance used by the onboarding host.
 */
@Composable
fun PairingContent(
    uiState: PairingUiState,
    onEvent: (PairingUiEvent) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
            .padding(top = 40.dp, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        PairingTopBar(
            title = title,
            linked = uiState.phase == PairingPhase.Paired,
            showBackButton = showBackButton,
            onBack = onBack,
        )

        when (uiState.phase) {
            PairingPhase.Loading, PairingPhase.Expired -> LoadingState()
            PairingPhase.WaitingForApproval -> CodeGeneratedState(
                code = uiState.code.orEmpty(),
                remainingSeconds = uiState.remainingSeconds,
                totalSeconds = uiState.totalSeconds,
            )
            PairingPhase.Rejected -> MessageState(
                message = stringResource(R.string.pairing_rejected),
                actionLabel = stringResource(R.string.pairing_generate_new),
                onAction = { onEvent(PairingUiEvent.GenerateCode) },
            )
            PairingPhase.Error -> when (uiState.error) {
                PairingError.Linking -> MessageState(
                    message = stringResource(R.string.pairing_link_error),
                    actionLabel = stringResource(R.string.pairing_generate_new),
                    onAction = { onEvent(PairingUiEvent.GenerateCode) },
                )
                PairingError.PairedDeviceLoad -> MessageState(
                    message = stringResource(R.string.pairing_paired_device_error),
                    actionLabel = stringResource(UiR.string.discovery_action_retry),
                    onAction = { onEvent(PairingUiEvent.RetryPairedDeviceLoad) },
                )
                PairingError.CodeGeneration, null -> MessageState(
                    message = stringResource(R.string.pairing_error),
                    actionLabel = stringResource(UiR.string.discovery_action_retry),
                    onAction = { onEvent(PairingUiEvent.GenerateCode) },
                )
            }
            PairingPhase.Paired -> uiState.device?.let { PairedState(device = it, onEvent = onEvent) }
        }
    }
}

// --- Shared top bar (title + optional back affordance + optional "Vinculado" chip) ---

@Composable
private fun PairingTopBar(
    title: String,
    linked: Boolean,
    showBackButton: Boolean,
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (showBackButton) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(UiR.drawable.ic_arrow_back),
                        contentDescription = stringResource(UiR.string.discovery_action_back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (linked) LinkedChip()
        }
        DiscoveryDivider(color = DiscoveryColors.Blue200)
    }
}

@Composable
private fun LinkedChip() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(DiscoveryColors.Green500))
        Text(
            text = stringResource(R.string.pairing_status_linked),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = DiscoveryColors.Green600,
        )
    }
}

// --- Loading ---

@Composable
private fun LoadingState() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Text(
            text = stringResource(R.string.pairing_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = DiscoveryColors.TextMuted,
            modifier = Modifier.padding(start = Spacing.md),
        )
    }
}

// --- Message (rejected / error) ---

@Composable
private fun MessageState(message: String, actionLabel: String, onAction: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        InfoBanner(text = message)
        Button(onClick = onAction) { Text(text = actionLabel) }
    }
}

// --- Código generado (Figma 1055-14806) ---

@Composable
private fun CodeGeneratedState(code: String, remainingSeconds: Long, totalSeconds: Long) {
    // Figma 1055-14806 stacks the heading block, the code card and the footer block with a 40px rhythm.
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(
                text = stringResource(R.string.pairing_heading),
                style = MaterialTheme.typography.headlineMedium,
                color = DiscoveryColors.Neutral900,
            )
            Text(
                text = stringResource(R.string.pairing_subheading),
                // DS "Body/XL" (Urbanist Regular 20); not in the shared scale, so derive from bodyMedium.
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 20.sp, lineHeight = 28.sp),
                color = DiscoveryColors.TextMuted,
            )
        }
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(SectionGap)) {
            CodeCard(modifier = Modifier.padding(horizontal = 52.dp), code = code, remainingSeconds = remainingSeconds, totalSeconds = totalSeconds)
            Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
                Text(
                    text = stringResource(R.string.pairing_footer),
                    style = MaterialTheme.typography.titleSmall,
                    color = DiscoveryColors.Neutral900,
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    HintRow(stringResource(R.string.pairing_hint_catalog))
                    HintRow(stringResource(R.string.pairing_hint_remote))
                }
            }
        }
    }
}

@Composable
private fun CodeCard(modifier: Modifier = Modifier, code: String, remainingSeconds: Long, totalSeconds: Long) {
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = DiscoveryColors.Blue50) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = SectionGap),
            verticalArrangement = Arrangement.spacedBy(SectionGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterHorizontally),
            ) {
                code.forEachIndexed { index, char -> CodeCell(char = char, index = index) }
            }
            // The bar and the "Expira en" caption form one group (Figma 1088:1626, 32px apart).
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                TtlProgressBar(remainingSeconds = remainingSeconds, totalSeconds = totalSeconds)
                ExpiresInCaption(remainingSeconds = remainingSeconds)
            }
        }
    }
}

@Composable
private fun ExpiresInCaption(remainingSeconds: Long) {
    val time = formatMmSs(remainingSeconds)
    val label = stringResource(R.string.pairing_expires_in, time)
    // Bold just the time portion in neutral-900, keeping the rest muted (Figma 1055:14999).
    val styled = buildAnnotatedString {
        val start = label.lastIndexOf(time)
        if (start < 0) {
            append(label)
        } else {
            append(label.substring(0, start))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = DiscoveryColors.Neutral900)) {
                append(time)
            }
            append(label.substring(start + time.length))
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_clock),
            contentDescription = stringResource(R.string.pairing_clock_cd),
            tint = DiscoveryColors.TextMuted,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = styled,
            style = MaterialTheme.typography.bodyMedium,
            color = DiscoveryColors.TextMuted,
            modifier = Modifier.padding(start = Spacing.sm),
        )
    }
}

@Composable
private fun CodeCell(char: Char, index: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.size(width = 104.dp, height = 124.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = char.toString(),
                // DS digit: Urbanist ExtraBold 64 (Figma 1055:14987); inline since it is unique to pairing.
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 48.sp,
                    lineHeight = 72.sp,
                ),
                color = DiscoveryColors.Neutral900,
            )
        }
    }
}

@Composable
private fun TtlProgressBar(remainingSeconds: Long, totalSeconds: Long) {
    // Fill against the TTL actually granted at generation; fall back to the documented max so the bar
    // never divides by zero if a code somehow arrives with no captured lifetime.
    val denominator = if (totalSeconds > 0) totalSeconds else PairingConstants.CODE_TTL_SECONDS
    val fraction = (remainingSeconds.toFloat() / denominator).coerceIn(0f, 1f)
    val barShape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(barShape)
            .background(DiscoveryColors.Divider),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(8.dp)
                .clip(barShape)
                .background(DiscoveryColors.AccentOrange),
        )
    }
}

@Composable
private fun HintRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_forward),
            contentDescription = null,
            tint = DiscoveryColors.AccentOrange,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = DiscoveryColors.TextMuted,
        )
    }
}

// --- Comercio vinculado (Figma 1080-1587) ---

@Composable
private fun PairedState(device: PairedDevice, onEvent: (PairingUiEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        SectionLabel(stringResource(R.string.pairing_store_section))
        InfoRowCard {
            Text(
                text = device.storeName ?: stringResource(R.string.pairing_pending),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.pairing_store_account_tag),
                style = MaterialTheme.typography.bodySmall,
                color = DiscoveryColors.TextMuted,
            )
        }

        SectionLabel(stringResource(R.string.pairing_device_section))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, DiscoveryColors.Divider),
        ) {
            Column {
                DetailRow(
                    label = stringResource(R.string.pairing_activity_label),
                    value = syncedLabel(device.lastSyncAt),
                )
                RowDivider()
                DetailRow(
                    label = stringResource(R.string.pairing_platform_label),
                    value = device.platform,
                )
                RowDivider()
                DetailRow(
                    label = stringResource(R.string.pairing_code_used_label),
                    value = device.pairingCode ?: stringResource(R.string.pairing_pending),
                )
            }
        }

        SectionLabel(stringResource(R.string.pairing_catalog_section))
        InfoBanner(text = stringResource(R.string.pairing_catalog_pending))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onEvent(PairingUiEvent.UnpairClicked) }
                .padding(vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_link),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.pairing_unpair),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = DiscoveryColors.TextMuted,
    )
}

@Composable
private fun InfoRowCard(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, DiscoveryColors.Divider),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = DiscoveryColors.TextMuted,
        )
    }
}

@Composable
private fun RowDivider() {
    DiscoveryDivider()
}

@Composable
private fun InfoBanner(text: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = DiscoveryColors.Blue50) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = DiscoveryColors.CatalogText,
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
        )
    }
}

/** Vertical rhythm between the pairing sections / inside the code card (Figma 40px, off the Spacing scale). */
private val SectionGap = 40.dp

/** Formats remaining seconds as `M:SS`. */
private fun formatMmSs(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(safe / 60, safe % 60)
}

/** Relative "synced N minutes ago" label; pending when no sync time is known. */
@Composable
private fun syncedLabel(lastSyncAt: Instant?): String {
    if (lastSyncAt == null) return stringResource(R.string.pairing_pending)
    val minutes = ChronoUnit.MINUTES.between(lastSyncAt, Instant.now()).coerceAtLeast(0)
    return if (minutes < 1) {
        stringResource(R.string.pairing_synced_recently)
    } else {
        pluralStringResource(R.plurals.pairing_synced_minutes, minutes.toInt(), minutes.toInt())
    }
}

// --- Previews (landscape kiosk) ---

private fun samplePairedDevice() = PairedDevice(
    deviceId = "dev-1",
    storeId = "store-1",
    deviceName = "Tablet Mostrador 1",
    platform = "ANDROID",
    status = "ACTIVE",
    lastSyncAt = Instant.now().minus(2, ChronoUnit.MINUTES),
    pairingCode = "2Y567",
    storeName = "Ferretería Don Pepe",
)

@Preview(name = "Pairing — code TIENDA (light)", showBackground = true, widthDp = 960, heightDp = 800)
@Composable
private fun PairingCodePreview() {
    DiscoveryTheme(darkTheme = false) {
        PairingContent(
            uiState = PairingUiState(
                phase = PairingPhase.WaitingForApproval,
                code = "2Y567",
                remainingSeconds = 52,
                totalSeconds = 145,
            ),
            onEvent = {},
            title = "TIENDA",
        )
    }
}

@Preview(name = "Pairing — code QHATHIA + back (dark)", showBackground = true, widthDp = 960, heightDp = 800)
@Composable
private fun PairingCodeOnboardingPreview() {
    DiscoveryTheme(darkTheme = true) {
        PairingContent(
            uiState = PairingUiState(
                phase = PairingPhase.WaitingForApproval,
                code = "2Y567",
                remainingSeconds = 52,
                totalSeconds = 145,
            ),
            onEvent = {},
            title = "QHATHIA",
            showBackButton = true,
        )
    }
}

@Preview(name = "Pairing — paired (dark)", showBackground = true, widthDp = 960, heightDp = 800)
@Composable
private fun PairingPairedPreview() {
    DiscoveryTheme(darkTheme = true) {
        PairingContent(
            uiState = PairingUiState(phase = PairingPhase.Paired, device = samplePairedDevice()),
            onEvent = {},
            title = "TIENDA",
        )
    }
}

@Preview(name = "Pairing — error (light)", showBackground = true, widthDp = 960, heightDp = 800)
@Composable
private fun PairingErrorPreview() {
    DiscoveryTheme(darkTheme = false) {
        PairingContent(uiState = PairingUiState(phase = PairingPhase.Error), onEvent = {}, title = "TIENDA")
    }
}

@Preview(name = "Pairing — link error (light)", showBackground = true, widthDp = 960, heightDp = 800)
@Composable
private fun PairingLinkErrorLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        PairingContent(
            uiState = PairingUiState(phase = PairingPhase.Error, error = PairingError.Linking),
            onEvent = {},
            title = "TIENDA",
        )
    }
}

@Preview(name = "Pairing — link error (dark)", showBackground = true, widthDp = 960, heightDp = 800)
@Composable
private fun PairingLinkErrorDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        PairingContent(
            uiState = PairingUiState(phase = PairingPhase.Error, error = PairingError.Linking),
            onEvent = {},
            title = "TIENDA",
        )
    }
}

@Preview(name = "Pairing — loading (dark)", showBackground = true, widthDp = 960, heightDp = 800)
@Composable
private fun PairingLoadingPreview() {
    DiscoveryTheme(darkTheme = true) {
        PairingContent(uiState = PairingUiState(phase = PairingPhase.Loading), onEvent = {}, title = "TIENDA")
    }
}

@Preview(name = "Pairing — paired device load error (light)", showBackground = true, widthDp = 960, heightDp = 800)
@Composable
private fun PairingPairedDeviceErrorLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        PairingContent(
            uiState = PairingUiState(phase = PairingPhase.Error, error = PairingError.PairedDeviceLoad),
            onEvent = {},
            title = "TIENDA",
        )
    }
}

@Preview(name = "Pairing — paired device load error (dark)", showBackground = true, widthDp = 960, heightDp = 800)
@Composable
private fun PairingPairedDeviceErrorDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        PairingContent(
            uiState = PairingUiState(phase = PairingPhase.Error, error = PairingError.PairedDeviceLoad),
            onEvent = {},
            title = "TIENDA",
        )
    }
}
