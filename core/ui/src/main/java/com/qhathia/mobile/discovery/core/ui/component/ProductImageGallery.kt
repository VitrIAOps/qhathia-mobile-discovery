package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing

/**
 * Design-system product image gallery for the detail screen (Figma `Detalle`): a large square main
 * image with a column of square lateral thumbnails. Business rule (Figma): when the product has fewer
 * than two photos, the lateral slots are filled with neutral gray placeholder squares.
 *
 * Domain-free — the caller passes already-resolved image URLs so the component stays reusable.
 *
 * @param imageUrls the product's image URLs, ordered (the first is the main image). May be empty.
 * @param mainContentDescription localized description for the main image (or null if decorative).
 * @param lateralSlots how many lateral thumbnail slots to render (filled with gray squares as needed).
 */
@Composable
fun ProductImageGallery(
    imageUrls: List<String>,
    mainContentDescription: String?,
    modifier: Modifier = Modifier,
    lateralSlots: Int = 3,
) {
    Row(
        modifier = modifier.height(GALLERY_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Main image — square, fills the gallery height (a neutral placeholder shows while empty/loading).
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(DiscoveryColors.Neutral50),
        ) {
            AsyncImage(
                model = imageUrls.firstOrNull(),
                contentDescription = mainContentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Lateral thumbnails: drop the main image, then pad with gray squares up to [lateralSlots].
        val thumbnails: List<String?> = imageUrls.drop(1).take(lateralSlots)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            repeat(lateralSlots) { index ->
                Thumbnail(url = thumbnails.getOrNull(index))
            }
        }
    }
}

@Composable
private fun Thumbnail(url: String?) {
    Box(
        modifier = Modifier
            .size(THUMBNAIL_SIZE)
            .clip(RoundedCornerShape(8.dp))
            .background(DiscoveryColors.Neutral50)
            .border(1.dp, DiscoveryColors.Blue200, RoundedCornerShape(8.dp)),
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null, // the main image carries the accessible label
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // url == null → a neutral gray placeholder square (the background above).
    }
}

private val GALLERY_HEIGHT = 348.dp
private val THUMBNAIL_SIZE = 88.dp

@Preview(name = "Gallery 3 photos — light", showBackground = true, widthDp = 520)
@Composable
private fun ProductImageGalleryMultiPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductImageGallery(
            imageUrls = listOf("a.jpg", "b.jpg", "c.jpg", "d.jpg"),
            mainContentDescription = "Taladro",
        )
    }
}

@Preview(name = "Gallery 1 photo (gray) — dark", showBackground = true, widthDp = 520)
@Composable
private fun ProductImageGallerySinglePreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductImageGallery(
            imageUrls = listOf("a.jpg"),
            mainContentDescription = "Taladro",
        )
    }
}
