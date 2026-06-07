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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
 * @param priceText already-formatted price (e.g. "S/ 380.00").
 * @param imageUrl primary image URL (a neutral placeholder shows while empty/loading).
 * @param cartContentDescription localized description for the cart button (required, non-null).
 * @param onCartClick invoked when the cart button is tapped.
 * @param onCardClick invoked when the card surface (outside the cart button) is tapped; when null the
 *   card is not clickable. Kept separate from [onCartClick] so the cart button retains its own tap.
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
) {
    Surface(
        modifier = modifier
            .width(CARD_WIDTH)
            .then(if (onCardClick != null) Modifier.clickable(onClick = onCardClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, DiscoveryColors.SearchBorder),
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
                Text(
                    text = priceText,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                CartButton(contentDescription = cartContentDescription, onClick = onCartClick)
            }
        }
    }
}

@Composable
private fun CartButton(
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

private val CARD_WIDTH = 176.dp
private val IMAGE_HEIGHT = 150.dp

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
