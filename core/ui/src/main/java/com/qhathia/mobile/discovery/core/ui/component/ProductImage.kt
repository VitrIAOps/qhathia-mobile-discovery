package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.qhathia.mobile.discovery.core.ui.R
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme

/**
 * A product photo with a neutral "no image" placeholder: a light background with a photo glyph when
 * [url] is `null` or blank, or when the image fails to load. The same background shows while the image
 * loads, so the area is never blank. Size and shape come from [modifier].
 *
 * @param url image URL; `null` or blank shows the placeholder.
 * @param contentDescription accessible label of the photo, also kept on the placeholder; `null` when a
 *   nearby text already names the product.
 * @param contentScale how the loaded image fits its area.
 */
@Composable
fun ProductImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    var failed by remember(url) { mutableStateOf(false) }
    val showPlaceholder = url.isNullOrBlank() || failed
    Box(
        modifier = modifier.background(DiscoveryColors.Neutral50),
        contentAlignment = Alignment.Center,
    ) {
        if (showPlaceholder) {
            Icon(
                painter = painterResource(R.drawable.ic_image),
                contentDescription = null,
                tint = DiscoveryColors.Neutral300,
                modifier = Modifier
                    .size(PLACEHOLDER_ICON_SIZE)
                    .then(
                        if (contentDescription != null) {
                            Modifier.semantics {
                                this.contentDescription = contentDescription
                                role = Role.Image
                            }
                        } else {
                            Modifier
                        },
                    ),
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = contentScale,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private val PLACEHOLDER_ICON_SIZE = 32.dp

@Preview(name = "ProductImage placeholder — light", showBackground = true)
@Composable
private fun ProductImagePlaceholderLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        ProductImage(
            url = null,
            contentDescription = "Taladro",
            modifier = Modifier.size(140.dp).clip(RoundedCornerShape(8.dp)),
        )
    }
}

@Preview(name = "ProductImage placeholder — dark", showBackground = true, backgroundColor = 0xFF0D1F38)
@Composable
private fun ProductImagePlaceholderDarkPreview() {
    DiscoveryTheme(darkTheme = true) {
        ProductImage(
            url = "",
            contentDescription = null,
            modifier = Modifier.size(140.dp).clip(RoundedCornerShape(8.dp)),
        )
    }
}
