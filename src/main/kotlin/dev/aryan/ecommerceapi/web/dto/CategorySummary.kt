package dev.aryan.ecommerceapi.web.dto

/** `GET /categories` response shape; also nested in [ProductDetailResponse.category]. */
data class CategorySummary(
    val id: Int,
    val slug: String,
    val name: String,
)
