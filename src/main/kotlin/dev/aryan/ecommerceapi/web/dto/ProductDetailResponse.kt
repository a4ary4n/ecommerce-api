package dev.aryan.ecommerceapi.web.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductDetailResponse(
    val id: Int,
    val title: String,
    val description: String?,
    val category: CategorySummary,
    val brand: String?,
    val sku: String?,
    val price: BigDecimal,
    val discountPercentage: BigDecimal?,
    val rating: BigDecimal?,
    val stock: Int?,
    val availabilityStatus: String,
    val weight: BigDecimal?,
    val width: BigDecimal?,
    val height: BigDecimal?,
    val depth: BigDecimal?,
    val warrantyInformation: String?,
    val shippingInformation: String?,
    val returnPolicy: String?,
    val minimumOrderQuantity: Int?,
    val thumbnail: String?,
    val barcode: String?,
    val qrCode: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val tags: List<String>,
    val images: List<ProductImageResponse>,
    val reviews: List<ReviewResponse>,
)
