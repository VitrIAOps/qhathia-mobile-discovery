package com.qhathia.mobile.discovery.core.domain.model

/**
 * The store's line of business (`StoreCategory` in the API), used to pick search examples that fit
 * the store. Unknown values are treated as "no category", so a category added by the backend later
 * never breaks the app.
 */
enum class StoreCategory {
    ELECTRONICS,
    LIGHTING,
    TEXTILE,
    FOOD,
    BEAUTY,
    JEWELRY,
    FURNITURE,
    DECORATION,
    TOOLS,
    SPORTS,
    BOOKS,
    TOYS,
    OTHER,
    ;

    companion object {
        /** Maps the API value; `null` for a missing, blank or unknown value. */
        fun fromApi(value: String?): StoreCategory? {
            val key = value?.trim()?.uppercase().orEmpty()
            return entries.firstOrNull { it.name == key }
        }
    }
}
