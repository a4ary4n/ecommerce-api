package dev.aryan.ecommerceapi.web.dto

import java.time.LocalDateTime

/** One product review, nested in [ProductDetailResponse.reviews]. */
data class ReviewResponse(
    val rating: Byte,
    val comment: String?,
    val reviewerName: String?,
    val reviewerEmail: String?,
    val reviewDate: LocalDateTime?,
)
