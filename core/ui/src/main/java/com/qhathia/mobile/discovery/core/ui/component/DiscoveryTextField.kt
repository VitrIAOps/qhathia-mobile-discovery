package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing

/**
 * A single-line labeled text field with a muted [label], a placeholder, an optional leading icon, an
 * error border, and an optional inline error message. Replaces the per-feature `PasswordField`
 * (Wi-Fi) and `FormField` (cart consumer data). Stateless — the caller owns the [value] and reacts to
 * [onValueChange].
 *
 * @param leadingIconRes optional icon shown before the input (e.g. user / phone glyphs).
 * @param isError when true, the border switches to the error color.
 * @param errorText optional message rendered below the field via [InlineErrorText].
 * @param visualTransformation e.g. `PasswordVisualTransformation()` to obscure the input.
 */
@Composable
fun DiscoveryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    leadingIconRes: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        // The field itself carries the label (read even while it is empty), so the visible label and
        // the placeholder are hidden from accessibility to avoid reading it twice.
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = DiscoveryColors.TextMuted,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = if (isError) MaterialTheme.colorScheme.error else DiscoveryColors.Divider,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (leadingIconRes != null) {
                Icon(
                    painter = painterResource(leadingIconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
                // The IME sends no pointer events, so each text change reports activity itself.
                val reportActivity = LocalUserActivity.current
                BasicTextField(
                    value = value,
                    onValueChange = {
                        reportActivity()
                        onValueChange(it)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = visualTransformation,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    // Label of the field, read also while it is empty (see DiscoverySearchBar).
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = label },
                )
            }
        }
        if (errorText != null) {
            InlineErrorText(text = errorText)
        }
    }
}

@Preview(name = "DiscoveryTextField — light", showBackground = true, widthDp = 320)
@Composable
private fun DiscoveryTextFieldLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        DiscoveryTextField(
            value = "Ana Pérez",
            onValueChange = {},
            label = "Nombres",
            placeholder = "Nombres",
        )
    }
}

@Preview(name = "DiscoveryTextField — error (dark)", showBackground = true, widthDp = 320, backgroundColor = 0xFF0D1F38)
@Composable
private fun DiscoveryTextFieldErrorPreview() {
    DiscoveryTheme(darkTheme = true) {
        DiscoveryTextField(
            value = "12",
            onValueChange = {},
            label = "Número de celular",
            placeholder = "Ej. 987 654 321",
            isError = true,
            errorText = "Ingresa un número de celular válido",
            keyboardType = KeyboardType.Phone,
        )
    }
}
