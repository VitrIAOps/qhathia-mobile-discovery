package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.runtime.key
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing

/**
 * A horizontally-scrolling grid that stacks up to [rows] items per column, scrolling sideways as
 * columns overflow — the chat catalog's "max 2 rows with horizontal scroll" layout.
 *
 * Items keep their natural height (each column is a plain [Column]), so cards are never squeezed to a
 * fraction of the viewport the way a fixed-row lazy grid would. Generic and domain-free; the caller
 * supplies a stable [key] and the per-item content.
 *
 * @param items the data to lay out.
 * @param rows number of items stacked vertically per column (e.g. 2 for the results grid).
 * @param key stable identity for each item (preserves composable identity across updates).
 * @param itemContent renders a single item.
 */
@Composable
fun <T> HorizontalRowsGrid(
    items: List<T>,
    rows: Int,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    // A few results read left to right in rows, as eyes and screen readers expect; only a set too big
    // for the screen scrolls sideways by columns (UX-T-13).
    if (items.size <= SMALL_RESULT_COUNT) {
        FlowRow(
            modifier = modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            maxItemsInEachRow = SMALL_RESULT_PER_ROW,
        ) {
            items.forEach { item -> key(key(item)) { itemContent(item) } }
        }
        return
    }
    val columns = items.chunked(rows)
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(items = columns, key = { column -> key(column.first()) }) { column ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                column.forEach { item -> itemContent(item) }
            }
        }
    }
}

/** Up to this many results are laid out in rows instead of scrolling columns. */
private const val SMALL_RESULT_COUNT = 6
private const val SMALL_RESULT_PER_ROW = 3

@Preview(name = "HorizontalRowsGrid — few, light", showBackground = true, backgroundColor = 0xFFF8F8F8, widthDp = 800, heightDp = 620)
@Composable
private fun HorizontalRowsGridFewPreview() {
    DiscoveryTheme(darkTheme = false) {
        HorizontalRowsGrid(items = (1..3).toList(), rows = 2, key = { it }) { index ->
            ProductCard(
                name = "Taladro inalámbrico DeWalt 12V $index",
                priceText = "S/ 380.00",
                imageUrl = "",
                cartContentDescription = "Agregar al carrito",
                onCartClick = {},
            )
        }
    }
}

@Preview(name = "HorizontalRowsGrid — light", showBackground = true, backgroundColor = 0xFFF8F8F8, widthDp = 800, heightDp = 620)
@Composable
private fun HorizontalRowsGridPreview() {
    DiscoveryTheme(darkTheme = false) {
        HorizontalRowsGrid(
            items = (1..8).toList(),
            rows = 2,
            key = { it },
        ) { index ->
            ProductCard(
                name = "Producto $index de ejemplo con nombre largo",
                priceText = "S/ 380.00",
                imageUrl = "",
                cartContentDescription = "Agregar al carrito",
                onCartClick = {},
            )
        }
    }
}
