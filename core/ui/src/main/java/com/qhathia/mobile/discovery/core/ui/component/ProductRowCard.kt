package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing

/**
 * Design-system horizontal product card (Figma `Product-cards`) used by the product-detail "related
 * products" strip: a square image on the left and a body on the right with the category tag, product
 * name, and price. Domain-free — the caller passes already-formatted primitives.
 *
 * @param name product display name.
 * @param categoryName category tag shown above the name (uppercased visually by the caller's data).
 * @param priceText already-formatted price (e.g. "S/ 380.00").
 * @param imageUrl primary image URL (a neutral placeholder shows while empty/loading).
 * @param onClick invoked when the card is tapped.
 */
@Composable
fun ProductRowCard(
    name: String,
    categoryName: String,
    priceText: String,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(CARD_WIDTH).height(CARD_HEIGHT).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, DiscoveryColors.SearchBorder),
    ) {
        Row {
            AsyncImage(
                model = imageUrl,
                contentDescription = null, // the name below is the accessible label
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(CARD_HEIGHT)
                    .background(DiscoveryColors.Neutral50),
            )
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 14.dp, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = categoryName.uppercase(),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
                    color = DiscoveryColors.AccentOrange,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = priceText,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

private val CARD_WIDTH = 300.dp
private val CARD_HEIGHT = 120.dp

@Preview(showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun ProductRowCardLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductRowCard(
            name = "Taladro stilson 14\"",
            categoryName = "Taladros",
            priceText = "S/ 380.00",
            imageUrl = "",
            onClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1F38)
@Composable
private fun ProductRowCardDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductRowCard(
            name = "Set de brocas para concreto (5 piezas)",
            categoryName = "Accesorios",
            priceText = "S/ 49.90",
            imageUrl = "",
            onClick = {},
        )
    }
}
