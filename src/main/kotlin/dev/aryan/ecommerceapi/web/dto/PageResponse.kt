package dev.aryan.ecommerceapi.web.dto

/** Generic pagination envelope for `GET /products`. */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
