package com.qhathia.mobile.discovery.feature.settings.ui

import com.qhathia.mobile.discovery.core.ui.component.labeled
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.settings.R
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEvent
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiState

/**
 * Stateless settings-PIN dialog UI — a pure function of [SettingsUiState], following the Figma DS
 * "Configuración protegida" node: an orange secure badge, a title + subtitle, PIN dots that fill as
 * digits are entered (at least four), and a numeric keypad (1–9, 0, confirm, backspace). The keypad
 * feeds digits up through [SettingsUiEvent.PasswordChanged]; confirm sends
 * [SettingsUiEvent.SubmitPassword] (enabled from 4 digits) and the 6th digit validates automatically.
 * After too many wrong PINs the keypad is disabled and the time left on the lock is shown (design D9).
 */
@Composable
fun SettingsPasswordDialogContent(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Header()
            Keypad(
                pinLength = uiState.passwordInput.length,
                dotCount = uiState.pinDotCount,
                isError = uiState.showError,
                isInputEnabled = uiState.isInputEnabled,
                isLocked = uiState.isLocked,
                lockedMinutesLeft = uiState.lockedMinutesLeft,
                isSubmitEnabled = uiState.isSubmitEnabled,
                onSubmit = { onEvent(SettingsUiEvent.SubmitPassword) },
                onDigit = { digit ->
                    onEvent(SettingsUiEvent.PasswordChanged(uiState.passwordInput + digit))
                },
                onBackspace = {
                    onEvent(SettingsUiEvent.PasswordChanged(uiState.passwordInput.dropLast(1)))
                },
            )
        }
    }
}

@Composable
private fun Header() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(DiscoveryColors.AccentOrange),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = stringResource(R.string.settings_password_icon_cd),
                tint = Color.White,
                modifier = Modifier.size(44.dp),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = stringResource(R.string.settings_password_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.settings_password_subtitle),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Keypad(
    pinLength: Int,
    dotCount: Int,
    isError: Boolean,
    isInputEnabled: Boolean,
    isLocked: Boolean,
    lockedMinutesLeft: Long?,
    isSubmitEnabled: Boolean,
    onSubmit: () -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    val digit: (String) -> Unit = { if (isInputEnabled) onDigit(it) }
    val backspace: () -> Unit = { if (isInputEnabled) onBackspace() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        PinDots(pinLength = pinLength, dotCount = dotCount, isError = isError)

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            KeyRow {
                DigitKey("1", digit, enabled = !isLocked)
                DigitKey("2", digit, enabled = !isLocked)
                DigitKey("3", digit, enabled = !isLocked)
            }
            KeyRow {
                DigitKey("4", digit, enabled = !isLocked)
                DigitKey("5", digit, enabled = !isLocked)
                DigitKey("6", digit, enabled = !isLocked)
            }
            KeyRow {
                DigitKey("7", digit, enabled = !isLocked)
                DigitKey("8", digit, enabled = !isLocked)
                DigitKey("9", digit, enabled = !isLocked)
            }
            KeyRow {
                ConfirmKey(enabled = isSubmitEnabled, onSubmit = onSubmit)
                DigitKey("0", digit, enabled = !isLocked)
                BackspaceKey(backspace, enabled = !isLocked)
            }
        }

        if (isLocked && lockedMinutesLeft != null) {
            Text(
                text = stringResource(R.string.settings_password_locked, formatHoursMinutes(lockedMinutesLeft)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        } else if (isError) {
            Text(
                text = stringResource(R.string.settings_password_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PinDots(pinLength: Int, dotCount: Int, isError: Boolean) {
    val ringColor = if (isError) MaterialTheme.colorScheme.error else DiscoveryColors.Teal400
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        repeat(dotCount) { index ->
            val filled = index < pinLength
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .then(
                        if (filled) {
                            Modifier.background(ringColor)
                        } else {
                            Modifier.border(2.dp, ringColor, CircleShape)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        content = content,
    )
}

@Composable
private fun RowScope.DigitKey(digit: String, onDigit: (String) -> Unit, enabled: Boolean = true) {
    KeyCell(label = digit, onClick = { onDigit(digit) }, enabled = enabled) {
        Text(
            text = digit,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else DISABLED_ALPHA),
        )
    }
}

@Composable
private fun RowScope.BackspaceKey(onBackspace: () -> Unit, enabled: Boolean = true) {
    KeyCell(label = stringResource(R.string.settings_pin_backspace_cd), onClick = onBackspace, enabled = enabled) {
        Icon(
            painter = painterResource(R.drawable.ic_backspace),
            contentDescription = null, // the label is on the key itself
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else DISABLED_ALPHA),
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun RowScope.ConfirmKey(enabled: Boolean, onSubmit: () -> Unit) {
    KeyCell(label = stringResource(R.string.settings_pin_confirm_cd), onClick = onSubmit, enabled = enabled) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null, // the label is on the key itself
            tint = if (enabled) DiscoveryColors.Teal400 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISABLED_ALPHA),
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun RowScope.KeyCell(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(KEY_HEIGHT),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, DiscoveryColors.Blue50),
        shadowElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier.clickable(enabled = enabled, onClick = onClick).labeled(label),
            contentAlignment = Alignment.Center,
            content = { content() },
        )
    }
}

/** Time left on the lock as "HH:mm" (e.g. 1439 → "23:59"). */
private fun formatHoursMinutes(totalMinutes: Long): String =
    String.format(Locale.ROOT, "%02d:%02d", totalMinutes / MINUTES_PER_HOUR, totalMinutes % MINUTES_PER_HOUR)

private const val MINUTES_PER_HOUR = 60L
private val KEY_HEIGHT = 56.dp
private const val DISABLED_ALPHA = 0.38f

@Preview(name = "Settings PIN — empty (light)", showBackground = true, widthDp = 360)
@Composable
private fun SettingsPasswordEmptyPreview() {
    DiscoveryTheme(darkTheme = false) {
        SettingsPasswordDialogContent(uiState = SettingsUiState(), onEvent = {})
    }
}

@Preview(name = "Settings PIN — typing (dark)", showBackground = true, widthDp = 360)
@Composable
private fun SettingsPasswordTypingPreview() {
    DiscoveryTheme(darkTheme = true) {
        SettingsPasswordDialogContent(uiState = SettingsUiState(passwordInput = "12"), onEvent = {})
    }
}

@Preview(name = "Settings PIN — 6 digits validating (light)", showBackground = true, widthDp = 360)
@Composable
private fun SettingsPasswordSixDigitsLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        SettingsPasswordDialogContent(
            uiState = SettingsUiState(passwordInput = "246810", isResolvingAccess = false, isValidating = true),
            onEvent = {},
        )
    }
}

@Preview(name = "Settings PIN — 4 digits, confirm enabled (dark)", showBackground = true, widthDp = 360)
@Composable
private fun SettingsPasswordConfirmDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        SettingsPasswordDialogContent(
            uiState = SettingsUiState(passwordInput = "2468", isResolvingAccess = false),
            onEvent = {},
        )
    }
}

@Preview(name = "Settings PIN — error (light)", showBackground = true, widthDp = 360)
@Composable
private fun SettingsPasswordErrorPreview() {
    DiscoveryTheme(darkTheme = false) {
        SettingsPasswordDialogContent(uiState = SettingsUiState(showError = true), onEvent = {})
    }
}

@Preview(name = "Settings PIN — locked (light)", showBackground = true, widthDp = 360)
@Composable
private fun SettingsPasswordLockedLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        SettingsPasswordDialogContent(
            uiState = SettingsUiState(isResolvingAccess = false, lockedMinutesLeft = 23 * 60 + 59),
            onEvent = {},
        )
    }
}

@Preview(name = "Settings PIN — locked (dark)", showBackground = true, widthDp = 360)
@Composable
private fun SettingsPasswordLockedDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        SettingsPasswordDialogContent(
            uiState = SettingsUiState(isResolvingAccess = false, lockedMinutesLeft = 75),
            onEvent = {},
        )
    }
}

@Preview(name = "Settings PIN — error (dark)", showBackground = true, widthDp = 360)
@Composable
private fun SettingsPasswordErrorDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        SettingsPasswordDialogContent(uiState = SettingsUiState(showError = true), onEvent = {})
    }
}
