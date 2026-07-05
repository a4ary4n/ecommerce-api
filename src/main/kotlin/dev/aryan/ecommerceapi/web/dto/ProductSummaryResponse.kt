package dev.aryan.ecommerceapi.web.dto

import java.time.LocalDateTime

// Mirrors ProductDocument - deliberately NOT full detail (full detail is /products/{id} from MySQL)
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
