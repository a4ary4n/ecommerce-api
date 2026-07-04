package dev.aryan.ecommerceapi.ingestion.dto

data class DummyJsonProductListResponse(
    val products: List<DummyJsonProductDto> = emptyList(),
    val total: Int = 0,
    val skip: Int = 0,
    val limit: Int = 0,
)

data class DummyJsonProductDto(
    val id: Int,
    val title: String,
    val description: String? = null,
    val category: String,
    val price: Double,
    val discountPercentage: Double? = null,
    val rating: Double? = null,
    val stock: Int? = null,
    val tags: List<String> = emptyList(),
    val brand: String? = null,
    val sku: String? = null,
    val weight: Double? = null,
    val dimensions: DummyJsonDimensionsDto? = null,
    val warrantyInformation: String? = null,
    val shippingInformation: String? = null,
    val availabilityStatus: String? = null,
    val reviews: List<DummyJsonReviewDto> = emptyList(),
    val returnPolicy: String? = null,
    val minimumOrderQuantity: Int? = null,
    val meta: DummyJsonMetaDto? = null,
    val images: List<String> = emptyList(),
    val thumbnail: String? = null,
)

data class DummyJsonDimensionsDto(
    val width: Double? = null,
    val height: Double? = null,
    val depth: Double? = null,
)

data class DummyJsonReviewDto(
    val rating: Int,
    val comment: String? = null,
    val date: String? = null,
    val reviewerName: String? = null,
    val reviewerEmail: String? = null,
)

data class DummyJsonMetaDto(
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val barcode: String? = null,
    val qrCode: String? = null,
)
