package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.qhathia.mobile.discovery.core.ui.R
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing

/**
 * Design-system horizontal product card (Figma `Product-cards`) used by the product-detail "related
 * products" strip: a square image on the left and a body on the right with the category tag, product
 * name, the price, and a dark circular add-to-cart button. Domain-free — the caller passes
 * already-formatted primitives.
 *
 * @param name product display name.
 * @param categoryName category tag shown above the name (uppercased visually by the caller's data).
 * @param priceText already-formatted price the customer pays (e.g. "S/ 380.00"); the discounted price
 *   when [originalPriceText] is set.
 * @param imageUrl primary image URL (a neutral placeholder shows while empty/loading).
 * @param cartContentDescription localized description for the add-to-cart button (required, non-null).
 * @param onClick invoked when the card surface (outside the cart button) is tapped.
 * @param onAddToCart invoked when the add-to-cart button is tapped. Kept separate from [onClick] so the
 *   button keeps its own tap while the card surface stays navigable.
 * @param originalPriceText already-formatted list price before the discount; when non-null it is shown
 *   struck through next to [priceText], which is highlighted.
 */
@Composable
fun ProductRowCard(
    name: String,
    categoryName: String,
    priceText: String,
    imageUrl: String,
    cartContentDescription: String,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
    originalPriceText: String? = null,
) {
    Box(
        modifier = modifier.width(CARD_WIDTH).height(CARD_HEIGHT).clickable(onClick = onClick)
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
                    .weight(1f)
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
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = priceText,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (originalPriceText != null) {
                                DiscoveryColors.AccentOrange
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                        )
                        if (originalPriceText != null) {
                            Text(
                                text = originalPriceText,
                                style = MaterialTheme.typography.bodySmall,
                                color = DiscoveryColors.Neutral500,
                                textDecoration = TextDecoration.LineThrough,
                                maxLines = 1,
                            )
                        }
                    }
                    AddToCartButton(contentDescription = cartContentDescription, onClick = onAddToCart)
                }
            }
        }
    }
}

@Composable
private fun AddToCartButton(
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(DiscoveryColors.CatalogText)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_cart),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
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
            cartContentDescription = "Agregar al carrito",
            onClick = {},
            onAddToCart = {},
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
            cartContentDescription = "Agregar al carrito",
            onClick = {},
            onAddToCart = {},
        )
    }
}

@Preview(name = "ProductRowCard discounted — light", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun ProductRowCardDiscountedLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductRowCard(
            name = "Foco LED Philips Globo E27 13W Luz Cálida",
            categoryName = "Iluminación",
            priceText = "S/ 30.90",
            originalPriceText = "S/ 35.90",
            imageUrl = "",
            cartContentDescription = "Agregar al carrito",
            onClick = {},
            onAddToCart = {},
        )
    }
}

@Preview(name = "ProductRowCard discounted — dark", showBackground = true, backgroundColor = 0xFF0D1F38)
@Composable
private fun ProductRowCardDiscountedDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductRowCard(
            name = "Foco LED Philips Globo E27 13W Luz Cálida",
            categoryName = "Iluminación",
            priceText = "S/ 30.90",
            originalPriceText = "S/ 35.90",
            imageUrl = "",
            cartContentDescription = "Agregar al carrito",
            onClick = {},
            onAddToCart = {},
        )
    }
}
