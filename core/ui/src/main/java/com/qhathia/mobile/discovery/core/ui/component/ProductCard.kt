package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.qhathia.mobile.discovery.core.ui.R
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing

/**
 * Design-system product card for the chat catalog: a white rounded card with the product image, name
 * (up to two lines), price, and a dark circular cart button. Domain-free — the caller passes already
 * formatted primitives so the card stays reusable.
 *
 * @param name product display name.
 * @param priceText already-formatted price the customer pays (e.g. "S/ 380.00"); the discounted price
 *   when [originalPriceText] is set.
 * @param imageUrl primary image URL (a neutral placeholder shows while empty/loading).
 * @param cartContentDescription localized description for the cart button (required, non-null).
 * @param onCartClick invoked when the cart button is tapped.
 * @param onCardClick invoked when the card surface (outside the cart button) is tapped; when null the
 *   card is not clickable. Kept separate from [onCartClick] so the cart button retains its own tap.
 * @param originalPriceText already-formatted list price before the discount; when non-null it is shown
 *   struck through above [priceText], which is highlighted.
 * @param cartEnabled `false` for a product that cannot be ordered: the cart button is dimmed and does
 *   nothing. The card itself stays tappable, so the detail can still be opened.
 */
@Composable
fun ProductCard(
    name: String,
    priceText: String,
    imageUrl: String,
    cartContentDescription: String,
    onCartClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null,
    originalPriceText: String? = null,
    cartEnabled: Boolean = true,
) {
    Surface(
        modifier = modifier
            .width(CARD_WIDTH)
            .then(if (onCardClick != null) Modifier.clickable(onClick = onCardClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null, // name below is the accessible label
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IMAGE_HEIGHT)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DiscoveryColors.Neutral50),
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    if (originalPriceText != null) {
                        Text(
                            text = originalPriceText,
                            style = MaterialTheme.typography.bodySmall,
                            color = DiscoveryColors.Neutral500,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                    Text(
                        text = priceText,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (originalPriceText != null) {
                            DiscoveryColors.AccentOrange
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
                CartButton(contentDescription = cartContentDescription, onClick = onCartClick, enabled = cartEnabled)
            }
        }
    }
}

@Composable
private fun CartButton(
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(CircleShape)
            .background(DiscoveryColors.CatalogText)
            .clickable(enabled = enabled, onClick = onClick),
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

private val CARD_WIDTH = 156.dp
private const val DISABLED_ALPHA = 0.38f
private val IMAGE_HEIGHT = 140.dp

@Preview(name = "ProductCard — light", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun ProductCardLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductCard(
            name = "Taladro Percutor Bosch GSB 13 RE Professional",
            priceText = "S/ 380.00",
            imageUrl = "",
            cartContentDescription = "Agregar al carrito",
            onCartClick = {},
        )
    }
}

@Preview(name = "ProductCard — dark", showBackground = true, backgroundColor = 0xFF0D1F38)
@Composable
private fun ProductCardDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductCard(
            name = "Lámpara de Pared Vintage",
            priceText = "S/ 89.90",
            imageUrl = "",
            cartContentDescription = "Agregar al carrito",
            onCartClick = {},
        )
    }
}

@Preview(name = "ProductCard discounted — light", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun ProductCardDiscountedLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductCard(
            name = "Foco LED Philips Globo E27 13W Luz Cálida",
            priceText = "S/ 30.90",
            originalPriceText = "S/ 35.90",
            imageUrl = "",
            cartContentDescription = "Agregar al carrito",
            onCartClick = {},
        )
    }
}

@Preview(name = "ProductCard discounted — dark", showBackground = true, backgroundColor = 0xFF0D1F38)
@Composable
private fun ProductCardDiscountedDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductCard(
            name = "Foco LED Philips Globo E27 13W Luz Cálida",
            priceText = "S/ 30.90",
            originalPriceText = "S/ 35.90",
            imageUrl = "",
            cartContentDescription = "Agregar al carrito",
            onCartClick = {},
        )
    }
}

@Preview(name = "ProductCard clickable — light", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun ProductCardClickablePreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductCard(
            name = "Taladro Percutor Bosch GSB 13 RE Professional",
            priceText = "S/ 380.00",
            imageUrl = "",
            cartContentDescription = "Agregar al carrito",
            onCartClick = {},
            onCardClick = {},
        )
    }
}

@Preview(name = "ProductCard — not orderable (light)", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun ProductCardNotOrderableLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductCard(
            name = "Taladro Percutor Bosch GSB 13 RE Professional",
            priceText = "S/ 380.00",
            imageUrl = "",
            cartContentDescription = "Agregar al carrito",
            onCartClick = {},
            cartEnabled = false,
        )
    }
}

@Preview(name = "ProductCard — not orderable (dark)", showBackground = true, backgroundColor = 0xFF122842)
@Composable
private fun ProductCardNotOrderableDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductCard(
            name = "Taladro Percutor Bosch GSB 13 RE Professional",
            priceText = "S/ 380.00",
            imageUrl = "",
            cartContentDescription = "Agregar al carrito",
            onCartClick = {},
            cartEnabled = false,
        )
    }
}
