package com.qhathia.mobile.discovery.core.ui.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** One formatter per thread: [DecimalFormat] is not thread-safe. */
private val PRICE_FORMAT = ThreadLocal.withInitial {
    // Comma for thousands and a point for cents whatever the device language, as in Qhathia Staff.
    DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
}

/** "S/ 1,250.00" for 1250.0 on any device (UX-X-04). */
fun formatPrice(amount: Double): String = "S/ " + PRICE_FORMAT.get()?.format(amount).orEmpty()
