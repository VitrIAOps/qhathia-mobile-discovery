package com.qhathia.mobile.discovery.core.domain.usecase

/** Most digits a cell-phone entry can hold: `51` prefix plus 9 digits ([com.qhathia.mobile.discovery.core.domain.CartConstants.PHONE_REGEX]). */
const val PHONE_MAX_DIGITS: Int = 11

/**
 * Keeps what a cell-phone field may contain (CAR-014): digits, spaces and one `+` before any digit.
 * Anything else (letters, `-`, `(`, the phone keypad's `N` / pause / wait) is dropped, and digits past
 * [PHONE_MAX_DIGITS] are ignored. Applied on every change, so pasted text is cleaned the same way.
 */
fun filterPhoneInput(raw: String): String {
    val out = StringBuilder()
    var digits = 0
    for (c in raw) {
        when {
            c.isDigit() && c in '0'..'9' -> if (digits < PHONE_MAX_DIGITS) {
                out.append(c)
                digits++
            }
            c == ' ' -> if (out.isNotEmpty() && out.last() != ' ') out.append(c)
            c == '+' -> if (out.isEmpty()) out.append(c)
        }
    }
    return out.toString()
}
