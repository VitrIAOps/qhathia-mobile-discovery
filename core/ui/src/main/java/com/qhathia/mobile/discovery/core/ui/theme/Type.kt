package com.qhathia.mobile.discovery.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.qhathia.mobile.discovery.core.ui.R

/** Urbanist — the Discovery DS type family (bundled variable font). */
@OptIn(ExperimentalTextApi::class)
val Urbanist: FontFamily = FontFamily(
    Font(R.font.urbanist_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.urbanist_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.urbanist_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.urbanist_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.urbanist_variable, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

/**
 * Type scale mapped to the Figma Home screen (kiosk / landscape tablet). Role mapping used by the
 * Home UI: headlineLarge = 48 hero, titleLarge = 40 listening label, titleMedium = 24 "TIENDA",
 * bodyLarge = 24 subtitle, bodyMedium = 16 search placeholder, labelLarge = 16 catalog link.
 */
val DiscoveryTypography: Typography = Typography(
    headlineLarge = TextStyle(fontFamily = Urbanist, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 56.sp),
    // DS "H1" — used by the product-detail price.
    headlineMedium = TextStyle(fontFamily = Urbanist, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = Urbanist, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 48.sp),
    titleMedium = TextStyle(fontFamily = Urbanist, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    // DS "H3" — used by the results headline and product price.
    titleSmall = TextStyle(fontFamily = Urbanist, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    bodyLarge = TextStyle(fontFamily = Urbanist, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 32.sp),
    bodyMedium = TextStyle(fontFamily = Urbanist, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    // DS "Body/MD" — used by the product card name.
    bodySmall = TextStyle(fontFamily = Urbanist, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Urbanist, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
)
