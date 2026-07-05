package dev.aryan.ecommerceapi.web

import dev.aryan.ecommerceapi.entity.Category
import dev.aryan.ecommerceapi.entity.Product
import dev.aryan.ecommerceapi.entity.ProductImage
import dev.aryan.ecommerceapi.entity.Review
import dev.aryan.ecommerceapi.search.ProductDocument
import dev.aryan.ecommerceapi.web.dto.CategorySummary
import dev.aryan.ecommerceapi.web.dto.ProductDetailResponse
import dev.aryan.ecommerceapi.web.dto.ProductImageResponse
import dev.aryan.ecommerceapi.web.dto.ProductSummaryResponse
import dev.aryan.ecommerceapi.web.dto.ReviewResponse

/** Maps a [Category] entity to its `GET /categories` / nested detail response shape. */
fun Category.toSummary(): CategorySummary = CategorySummary(
    id = id!!, // IDENTITY-generated, always populated once persisted/read back
    slug = slug,
    name = name,
)

/** Maps a [Review] entity to its `GET /products/{id}` response shape. */
fun Review.toResponse(): ReviewResponse = ReviewResponse(
    rating = rating,
    comment = comment,
    reviewerName = reviewerName,
    reviewerEmail = reviewerEmail,
    reviewDate = reviewDate,
)

/** Maps a [ProductImage] entity to its `GET /products/{id}` response shape. */
fun ProductImage.toResponse(): ProductImageResponse = ProductImageResponse(
    url = url,
    sortOrder = sortOrder,
)

/** Maps a [ProductDocument] search hit to `GET /products`'s list-result response shape. */
fun ProductDocument.toSummaryResponse(): ProductSummaryResponse = ProductSummaryResponse(
    id = id.toInt(),
    title = title,
    description = description,
    category = category,
    brand = brand,
    tags = tags,
    price = price,
    discountPercentage = discountPercentage,
    rating = rating,
    stock = stock,
    availabilityStatus = availabilityStatus,
    thumbnail = thumbnail,
    createdAt = createdAt,
)

/**
 * Maps a [Product] entity (plus its separately-fetched [reviews]/[images]/[tags], since
 * `Product` deliberately has no collection fields for these) to `GET /products/{id}`'s
 * full-detail response shape.
 */
fun Product.toDetailResponse(
    reviews: List<Review>,
    images: List<ProductImage>,
    tags: List<String>,
): ProductDetailResponse = ProductDetailResponse(
    id = id,
    title = title,
    description = description,
    category = category.toSummary(),
    brand = brand?.name,
    sku = sku,
    price = price,
    discountPercentage = discountPercentage,
    rating = rating,
    stock = stock,
    availabilityStatus = availabilityStatus.name,
    weight = weight,
    width = width,
    height = height,
    depth = depth,
    warrantyInformation = warrantyInformation,
    shippingInformation = shippingInformation,
    returnPolicy = returnPolicy,
    minimumOrderQuantity = minimumOrderQuantity,
    thumbnail = thumbnail,
    barcode = barcode,
    qrCode = qrCode,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tags = tags,
    images = images.map { it.toResponse() },
    reviews = reviews.map { it.toResponse() },
)
