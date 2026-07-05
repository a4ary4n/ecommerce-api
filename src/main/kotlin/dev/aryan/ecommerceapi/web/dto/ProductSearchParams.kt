package dev.aryan.ecommerceapi.web.dto

data class ProductSearchParams(
    val query: String?,
    val category: String?,
    val brand: String?,
    val minPrice: Double?,
    val maxPrice: Double?,
    val minRating: Double?,
    val inStock: Boolean?,
    val sort: String?,
    val page: Int,
    val size: Int,
)
