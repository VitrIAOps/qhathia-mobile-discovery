package com.qhathia.mobile.discovery.core.domain.model

/**
 * A single product photo in the detail gallery. Framework-free domain model.
 *
 * @property url image URL.
 * @property position ordering position within the product's media (ascending).
 * @property isPrimary whether this is the product's primary image.
 */
data class ProductImage(
    val url: String,
    val position: Int,
    val isPrimary: Boolean,
)
