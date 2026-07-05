package dev.aryan.ecommerceapi.ingestion

import dev.aryan.ecommerceapi.entity.AvailabilityStatus
import dev.aryan.ecommerceapi.entity.Brand
import dev.aryan.ecommerceapi.entity.Category
import dev.aryan.ecommerceapi.entity.Product
import dev.aryan.ecommerceapi.entity.ProductImage
import dev.aryan.ecommerceapi.entity.ProductTag
import dev.aryan.ecommerceapi.entity.ProductTagId
import dev.aryan.ecommerceapi.entity.Review
import dev.aryan.ecommerceapi.entity.Tag
import dev.aryan.ecommerceapi.ingestion.dto.DummyJsonProductDto
import dev.aryan.ecommerceapi.ingestion.dto.DummyJsonReviewDto
import dev.aryan.ecommerceapi.search.ProductDocument
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Maps dummyjson's display strings (e.g. "In Stock") to [AvailabilityStatus], or `null` if unrecognized. */
fun String?.toAvailabilityStatus(): AvailabilityStatus? = when (this?.lowercase()) {
    "in stock" -> AvailabilityStatus.IN_STOCK
    "low stock" -> AvailabilityStatus.LOW_STOCK
    "out of stock" -> AvailabilityStatus.OUT_OF_STOCK
    else -> null
}

/**
 * Parses a dummyjson ISO-8601 timestamp (trailing "Z"/UTC) into [LocalDateTime].
 * Explicit, rather than trusting Jackson 3's default java-time handling on
 * `RestClient`'s converters.
 */
fun String?.toSourceTimestamp(): LocalDateTime? =
    this?.let { LocalDateTime.ofInstant(Instant.parse(it), ZoneOffset.UTC) }

/** Maps a raw dummyjson product payload to a [Product] entity, ready to persist. */
fun DummyJsonProductDto.toEntity(
    category: Category,
    brand: Brand?,
    status: AvailabilityStatus,
    ingestedAt: LocalDateTime,
): Product = Product(
    id = id,
    title = title,
    description = description,
    category = category,
    brand = brand,
    sku = sku,
    // BigDecimal.valueOf(double), never Double.toBigDecimal() (unlimited precision on
    // the double's exact binary value) - the latter turns 19.99 into visible float noise.
    price = BigDecimal.valueOf(price),
    discountPercentage = discountPercentage?.let(BigDecimal::valueOf),
    rating = rating?.let(BigDecimal::valueOf),
    stock = stock,
    availabilityStatus = status,
    weight = weight?.let(BigDecimal::valueOf),
    width = dimensions?.width?.let(BigDecimal::valueOf),
    height = dimensions?.height?.let(BigDecimal::valueOf),
    depth = dimensions?.depth?.let(BigDecimal::valueOf),
    warrantyInformation = warrantyInformation,
    shippingInformation = shippingInformation,
    returnPolicy = returnPolicy,
    minimumOrderQuantity = minimumOrderQuantity,
    thumbnail = thumbnail,
    barcode = meta?.barcode,
    qrCode = meta?.qrCode,
    createdAt = meta?.createdAt.toSourceTimestamp(), // source's own timestamp
    updatedAt = ingestedAt,                           // our system's last-touched time
)

/** Maps a raw dummyjson review payload to a [Review] entity attached to [product]. */
fun DummyJsonReviewDto.toEntity(product: Product): Review = Review(
    product = product,
    rating = rating.toByte(),
    comment = comment,
    reviewerName = reviewerName,
    reviewerEmail = reviewerEmail,
    reviewDate = date.toSourceTimestamp(),
)

/** Wraps this URL as a [ProductImage] entity attached to [product] at position [sortOrder]. */
fun String.toImageEntity(product: Product, sortOrder: Int): ProductImage = ProductImage(
    product = product,
    url = this,
    sortOrder = sortOrder.toShort(),
)

/** Creates the [ProductTag] join row linking this product to [tag]. */
fun Product.linkTag(tag: Tag): ProductTag =
    ProductTag(ProductTagId(id, tag.id!!), this, tag)

/** Flattens this product (plus its resolved [tags]) into a [ProductDocument] for indexing. */
fun Product.toDocument(tags: List<String>): ProductDocument = ProductDocument(
    id = id.toString(),
    title = title,
    description = description,
    category = category.slug,
    brand = brand?.name,
    tags = tags,
    price = price.toDouble(),
    discountPercentage = discountPercentage?.toDouble(),
    rating = rating?.toDouble(),
    stock = stock,
    availabilityStatus = availabilityStatus.name,
    thumbnail = thumbnail,
    createdAt = createdAt,
)
