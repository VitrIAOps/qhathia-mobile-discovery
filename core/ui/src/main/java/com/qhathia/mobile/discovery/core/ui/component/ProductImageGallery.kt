package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.R
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing

/**
 * Design-system product image gallery for the detail screen (Figma `Detalle`): a large square main
 * image with a column of square lateral thumbnails, one per extra photo (up to [lateralSlots]). Empty
 * slots are not drawn (F-43, DET-062): a product with one photo shows no thumbnails, and a product
 * with none shows the [ProductImage] placeholder with "Sin foto". A photo that fails to load shows
 * the placeholder without the label.
 *
 * Domain-free — the caller passes already-resolved image URLs so the component stays reusable.
 *
 * @param imageUrls the product's image URLs, ordered (the first is the main image). May be empty.
 * @param productName product name, used for the photo descriptions ("Foto 2 de Taladro…").
 * @param lateralSlots the most lateral thumbnails to show.
 */
@Composable
fun ProductImageGallery(
    imageUrls: List<String>,
    productName: String,
    modifier: Modifier = Modifier,
    lateralSlots: Int = 3,
) {
    Row(
        modifier = modifier.height(GALLERY_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Main image — square, fills the gallery height.
        ProductImage(
            url = imageUrls.firstOrNull(),
            contentDescription = stringResource(R.string.discovery_cd_photo_of, 1, productName),
            contentScale = ContentScale.Crop,
            showMissingLabel = imageUrls.isEmpty(),
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp)),
        )

        // Lateral thumbnails: only the photos that exist after the main one, never empty squares.
        val thumbnails = imageUrls.drop(1).take(lateralSlots)
        if (thumbnails.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                thumbnails.forEachIndexed { index, url ->
                    // Photo numbers count the main image as 1, so the first thumbnail is photo 2.
                    Thumbnail(url = url, photoNumber = index + 2, productName = productName)
                }
            }
        }
    }
}

@Composable
private fun Thumbnail(url: String, photoNumber: Int, productName: String) {
    val shape = RoundedCornerShape(8.dp)
    ProductImage(
        url = url,
        contentDescription = stringResource(R.string.discovery_cd_photo_of, photoNumber, productName),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(THUMBNAIL_SIZE)
            .clip(shape)
            .border(1.dp, DiscoveryColors.Blue200, shape),
    )
}

private val GALLERY_HEIGHT = 348.dp
private val THUMBNAIL_SIZE = 88.dp

@Preview(name = "Gallery 3 photos — light", showBackground = true, widthDp = 520)
@Composable
private fun ProductImageGalleryMultiPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductImageGallery(
            imageUrls = listOf("a.jpg", "b.jpg", "c.jpg", "d.jpg"),
            productName = "Taladro",
        )
    }
}

@Preview(name = "Gallery 1 photo, no thumbnails — dark", showBackground = true, widthDp = 520)
@Composable
private fun ProductImageGallerySinglePreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductImageGallery(
            imageUrls = listOf("a.jpg"),
            productName = "Taladro",
        )
    }
}

@Preview(name = "Gallery no photo (Sin foto) — light", showBackground = true, widthDp = 520)
@Composable
private fun ProductImageGalleryEmptyLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductImageGallery(imageUrls = emptyList(), productName = "Foco")
    }
}

@Preview(name = "Gallery no photo (Sin foto) — dark", showBackground = true, widthDp = 520)
@Composable
private fun ProductImageGalleryEmptyDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductImageGallery(imageUrls = emptyList(), productName = "Foco")
    }
}
