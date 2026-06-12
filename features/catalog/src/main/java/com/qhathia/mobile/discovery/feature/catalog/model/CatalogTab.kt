package com.qhathia.mobile.discovery.feature.catalog.model

/**
 * A selectable tab in the catalog's category bar. The bar always opens with [Kind.All] ("Todo") first
 * and ends with [Kind.Discounts] ("Descuentos", shown with the discount icon); one [Kind.Category] tab
 * sits in between for each store category returned by the API.
 *
 * @property id stable identity (`all`, `discounts`, or `category:<categoryId>`) for selection + keys.
 * @property kind which family of tab this is; drives the label source and the discount icon.
 * @property label the display label for a [Kind.Category] tab (the category name); empty for the
 *   fixed tabs, whose labels come from string resources.
 */
data class CatalogTab(
    val id: String,
    val kind: Kind,
    val label: String = "",
) {
    enum class Kind { All, Category, Discounts }

    companion object {
        const val ALL_ID: String = "all"
        const val DISCOUNTS_ID: String = "discounts"
        fun categoryId(categoryId: String): String = "category:$categoryId"
    }
}
