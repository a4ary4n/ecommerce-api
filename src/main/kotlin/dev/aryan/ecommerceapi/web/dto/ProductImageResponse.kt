package dev.aryan.ecommerceapi.web.dto

/** One product image, nested in [ProductDetailResponse.images]. */
data class ProductImageResponse(
    val url: String,
    val sortOrder: Short?,
)
