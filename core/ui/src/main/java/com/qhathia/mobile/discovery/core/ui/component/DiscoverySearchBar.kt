package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing

/**
 * Stateless search input for the Home screen — a white pill with a leading search icon, matching
 * the Qhathia DS. The query is fully hoisted to the caller.
 *
 * @param query current text value.
 * @param onQueryChange invoked as the user types.
 * @param onSearch invoked when the user submits the query (IME action).
 * @param placeholder placeholder text shown when [query] is empty.
 * @param searchIconContentDescription localized description for the leading search icon.
 */
@Composable
fun DiscoverySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    placeholder: String,
    searchIconContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    // The search screens live in a single Activity window, so `windowSoftInputMode=stateHidden`
    // only suppresses the keyboard on the very first screen. A focus eater claims the window's
    // initial focus on entry so the text field never auto-focuses — the keyboard opens only when
    // the user taps the field.
    val focusEater = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusEater.requestFocus() }
    Box(modifier = modifier) {
        // Invisible target that absorbs the initial focus instead of the text field below.
        Spacer(
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusEater)
                .focusable(),
        )
        // The whole pill is the text field (decorated via decorationBox), so tapping anywhere on
        // it — icon, placeholder or empty space — focuses the field and opens the keyboard.
        // The IME sends no pointer events, so each text change reports activity itself.
        val reportActivity = LocalUserActivity.current
        BasicTextField(
            value = query,
            onValueChange = {
                reportActivity()
                onQueryChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 49.dp)
                .background(color = Color.White, shape = shape)
                .border(width = 1.dp, color = DiscoveryColors.SearchBorder, shape = shape),
            singleLine = true,
            textStyle = LocalTextStyle.current.merge(
                MaterialTheme.typography.bodyMedium.copy(color = DiscoveryColors.Blue800),
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .heightIn(min = 49.dp)
                        .padding(horizontal = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = searchIconContentDescription,
                        tint = DiscoveryColors.Neutral500,
                        modifier = Modifier.size(24.dp),
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = Spacing.sm)
                            .weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = DiscoveryColors.Neutral500,
                            )
                        }
                        innerTextField()
                    }
                }
            },
        )
    }
}

@Preview(name = "SearchBar empty — light", showBackground = true, backgroundColor = 0xFFF8F8F8, widthDp = 600)
@Composable
private fun DiscoverySearchBarEmptyPreview() {
    DiscoveryTheme(darkTheme = false) {
        DiscoverySearchBar(
            query = "",
            onQueryChange = {},
            onSearch = {},
            placeholder = "Describe lo que buscas (taladro de la marca bosch)",
            searchIconContentDescription = "Buscar",
        )
    }
}

@Preview(name = "SearchBar filled — dark", showBackground = true, widthDp = 600)
@Composable
private fun DiscoverySearchBarFilledPreview() {
    DiscoveryTheme(darkTheme = true) {
        DiscoverySearchBar(
            query = "Taladro Bosch",
            onQueryChange = {},
            onSearch = {},
            placeholder = "Describe lo que buscas",
            searchIconContentDescription = "Buscar",
        )
    }
}
