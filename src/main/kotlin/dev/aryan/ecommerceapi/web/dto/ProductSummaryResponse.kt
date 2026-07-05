package dev.aryan.ecommerceapi.web.dto

import java.time.LocalDateTime

/**
 * `GET /products` list/search result shape - mirrors
 * [ProductDocument][dev.aryan.ecommerceapi.search.ProductDocument] field-for-field.
 * Deliberately NOT full detail; see [ProductDetailResponse] for that (`GET /products/{id}`, from MySQL).
 */
data class ProductSummaryResponse(
    val id: Int,
    val title: String,
    val description: String?,
    val category: String,
    val brand: String?,
    val tags: List<String>,
    val price: Double,
    val discountPercentage: Double?,
    val rating: Double?,
    val stock: Int?,
    val availabilityStatus: String,
    val thumbnail: String?,
    val createdAt: LocalDateTime?,
)
