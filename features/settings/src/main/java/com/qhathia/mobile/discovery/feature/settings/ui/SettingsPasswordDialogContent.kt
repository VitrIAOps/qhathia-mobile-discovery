package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import com.qhathia.mobile.discovery.core.domain.SettingsConstants
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.settings.R
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEvent
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiState

/**
 * Stateless settings-PIN dialog UI — a pure function of [SettingsUiState], following the Figma DS
 * "Configuración protegida" node: an orange secure badge, a title + subtitle, four PIN dots that fill
 * as digits are entered, and a numeric keypad (1–9, 0, backspace). The keypad feeds digits up through
 * [SettingsUiEvent.PasswordChanged]; the ViewModel validates automatically once the PIN is complete.
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
                isError = uiState.showError,
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
    isError: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        PinDots(pinLength = pinLength, isError = isError)

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            KeyRow {
                DigitKey("1", onDigit)
                DigitKey("2", onDigit)
                DigitKey("3", onDigit)
            }
            KeyRow {
                DigitKey("4", onDigit)
                DigitKey("5", onDigit)
                DigitKey("6", onDigit)
            }
            KeyRow {
                DigitKey("7", onDigit)
                DigitKey("8", onDigit)
                DigitKey("9", onDigit)
            }
            KeyRow {
                Spacer(modifier = Modifier.weight(1f))
                DigitKey("0", onDigit)
                BackspaceKey(onBackspace)
            }
        }

        if (isError) {
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
private fun PinDots(pinLength: Int, isError: Boolean) {
    val ringColor = if (isError) MaterialTheme.colorScheme.error else DiscoveryColors.Teal400
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        repeat(SettingsConstants.SETTINGS_PASSWORD_LENGTH) { index ->
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
private fun RowScope.DigitKey(digit: String, onDigit: (String) -> Unit) {
    KeyCell(onClick = { onDigit(digit) }) {
        Text(
            text = digit,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RowScope.BackspaceKey(onBackspace: () -> Unit) {
    KeyCell(onClick = onBackspace) {
        Icon(
            painter = painterResource(R.drawable.ic_backspace),
            contentDescription = stringResource(R.string.settings_pin_backspace_cd),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun RowScope.KeyCell(onClick: () -> Unit, content: @Composable () -> Unit) {
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
            modifier = Modifier.clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
            content = { content() },
        )
    }
}

private val KEY_HEIGHT = 56.dp

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

@Preview(name = "Settings PIN — error (light)", showBackground = true, widthDp = 360)
@Composable
private fun SettingsPasswordErrorPreview() {
    DiscoveryTheme(darkTheme = false) {
        SettingsPasswordDialogContent(uiState = SettingsUiState(showError = true), onEvent = {})
    }
}
