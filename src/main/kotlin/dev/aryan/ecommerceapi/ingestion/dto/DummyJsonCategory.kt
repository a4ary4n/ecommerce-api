package dev.aryan.ecommerceapi.ingestion.dto

/** Raw shape of one entry from dummyjson's `/products/categories` endpoint. [url] is read but never persisted. */
data class DummyJsonCategoryDto(
    val slug: String,
    val name: String,
    val url: String? = null,
)
