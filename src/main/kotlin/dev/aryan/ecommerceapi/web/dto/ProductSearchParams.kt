package dev.aryan.ecommerceapi.web.dto

data class ProductSearchParams(
    val query: String?,
    val category: String?,
    val page: Int,
    val size: Int,
)
