package com.qhathia.mobile.discovery.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Exact color tokens from the Qhathia design system (Figma `Qhatia-DS`, node 286-9269).
 * Primitive names mirror the Figma variables.
 */
object DiscoveryColors {
    // Primitives (from Figma variable defs)
    val Blue600 = Color(0xFF163550) // headings ("Encuentra lo que necesitas")
    val Blue700 = Color(0xFF112947) // primitive-blue-700 — "Agregar al carrito" button
    val Blue800 = Color(0xFF0D1F38) // "TIENDA"
    val Blue200 = Color(0xFF95AABF) // primitive-blue-200 — product-detail thumbnail border
    val Green500 = Color(0xFF22C55E) // primitive-green-500 — in-stock + "Compartir" button
    val Neutral50 = Color(0xFFF8F8F8) // screen surface
    val Neutral500 = Color(0xFF757575) // search placeholder
    val CatalogText = Color(0xFF0A2540) // "Ir al catálogo" / detail title & price
    val TextMuted = Color(0xFF616161) // product-detail description
    val Divider = Color(0xFFE0E0E0) // product-detail info divider
    val AccentOrange = Color(0xFFFF4500) // product card category tag
    val SearchBorder = Color(0xFFDBDBDB)
    val Footer = Color(0xFF878787) // "powered by Qhathia"

    // Brand accent — the microphone gradient + glow (raster in Figma, recreated in Compose).
    // Slightly deeper / more saturated orange so the disc reads stronger on the light surface.
    val MicGradientStart = Color(0xFFE25400) // deep orange
    val MicGradientEnd = Color(0xFFFF8A0F) // amber-orange
    val MicGlow = Color(0xFFFF6A00) // saturated glow/halo

    // Dark-mode surface (the DS ships light only; this is a derived dark variant)
    val DarkSurface = Color(0xFF122842)
    val DarkBackground = Color(0xFF0D1F38)
    val DarkOnSurface = Color(0xFFF1F4F8)
    val DarkOnSurfaceVariant = Color(0xFFB2B9C4)
    val DarkOutline = Color(0xFF38496A)
}
