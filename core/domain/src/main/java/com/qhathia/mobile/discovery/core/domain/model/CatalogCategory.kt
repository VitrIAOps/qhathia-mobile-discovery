package com.qhathia.mobile.discovery.core.domain.model

/**
 * A store category with its available products, as returned by the catalog endpoint. Framework-free
 * domain model. The backend already filters to categories that have `AVAILABLE` products and orders
 * both the categories and their products, so the list is presentation-ready.
 *
 * @property id stable category identifier.
 * @property name display name.
 * @property products the category's available products, in display order.
 */
data class CatalogCategory(
    val id: String,
    val name: String,
    val products: List<Product>,
)
