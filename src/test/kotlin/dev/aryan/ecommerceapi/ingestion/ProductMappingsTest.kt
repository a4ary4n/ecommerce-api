package dev.aryan.ecommerceapi.ingestion

import dev.aryan.ecommerceapi.entity.AvailabilityStatus
import dev.aryan.ecommerceapi.entity.Brand
import dev.aryan.ecommerceapi.entity.Category
import dev.aryan.ecommerceapi.entity.Tag
import dev.aryan.ecommerceapi.ingestion.dto.DummyJsonDimensionsDto
import dev.aryan.ecommerceapi.ingestion.dto.DummyJsonMetaDto
import dev.aryan.ecommerceapi.ingestion.dto.DummyJsonProductDto
import dev.aryan.ecommerceapi.ingestion.dto.DummyJsonReviewDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Unit tests for the DTO -> entity -> [dev.aryan.ecommerceapi.search.ProductDocument] mapping
 * extension functions - pure functions, no Spring context needed. Covers the specific
 * conversion traps documented in CLAUDE.md (BigDecimal float noise, brand nullability,
 * enum normalization, source-vs-ingestion timestamps).
 */
class ProductMappingsTest {

    private val category = Category(id = 1, slug = "smartphones", name = "Smartphones")
    private val brand = Brand(id = 1, name = "Apple")

    private fun productDto(
        price: Double = 19.99,
        discountPercentage: Double? = 12.5,
        rating: Double? = 4.5,
        weight: Double? = 2.0,
        dimensions: DummyJsonDimensionsDto? = DummyJsonDimensionsDto(1.0, 2.0, 3.0),
        meta: DummyJsonMetaDto? = DummyJsonMetaDto(createdAt = "2024-01-15T09:30:00.000Z"),
    ) = DummyJsonProductDto(
        id = 1,
        title = "iPhone",
        category = "smartphones",
        price = price,
        discountPercentage = discountPercentage,
        rating = rating,
        weight = weight,
        dimensions = dimensions,
        meta = meta,
    )

    @Test
    fun `toAvailabilityStatus maps known display strings case-insensitively`() {
        assertEquals(AvailabilityStatus.IN_STOCK, "In Stock".toAvailabilityStatus())
        assertEquals(AvailabilityStatus.IN_STOCK, "IN STOCK".toAvailabilityStatus())
        assertEquals(AvailabilityStatus.LOW_STOCK, "Low Stock".toAvailabilityStatus())
        assertEquals(AvailabilityStatus.OUT_OF_STOCK, "Out of Stock".toAvailabilityStatus())
    }

    @Test
    fun `toAvailabilityStatus returns null for unrecognized or missing values`() {
        assertNull("Discontinued".toAvailabilityStatus())
        assertNull(null.toAvailabilityStatus())
    }

    @Test
    fun `toSourceTimestamp parses an ISO-8601 UTC timestamp`() {
        val parsed = "2024-01-15T09:30:00.000Z".toSourceTimestamp()
        assertEquals(LocalDateTime.of(2024, 1, 15, 9, 30, 0), parsed)
    }

    @Test
    fun `toSourceTimestamp returns null for null input`() {
        assertNull((null as String?).toSourceTimestamp())
    }

    @Test
    fun `toEntity converts price via BigDecimal-valueOf, never raw double conversion`() {
        val product = productDto(price = 19.99).toEntity(category, brand, AvailabilityStatus.IN_STOCK, LocalDateTime.now())
        // BigDecimal.valueOf(19.99) == BigDecimal("19.99") exactly; Double.toBigDecimal() would
        // instead produce visible float noise like 19.9899999999999999911182158029987476766109466552734375
        assertEquals(BigDecimal("19.99"), product.price)
    }

    @Test
    fun `toEntity leaves nullable numeric fields null when the source omits them`() {
        val product = productDto(discountPercentage = null, rating = null, weight = null, dimensions = null)
            .toEntity(category, brand, AvailabilityStatus.IN_STOCK, LocalDateTime.now())
        assertNull(product.discountPercentage)
        assertNull(product.rating)
        assertNull(product.weight)
        assertNull(product.width)
        assertNull(product.height)
        assertNull(product.depth)
    }

    @Test
    fun `toEntity leaves brand null for brandless products`() {
        val product = productDto().toEntity(category, brand = null, status = AvailabilityStatus.IN_STOCK, ingestedAt = LocalDateTime.now())
        assertNull(product.brand)
    }

    @Test
    fun `toEntity uses the source's own createdAt, never fabricates it`() {
        val product = productDto(meta = DummyJsonMetaDto(createdAt = "2024-01-15T09:30:00.000Z"))
            .toEntity(category, brand, AvailabilityStatus.IN_STOCK, LocalDateTime.now())
        assertEquals(LocalDateTime.of(2024, 1, 15, 9, 30, 0), product.createdAt)
    }

    @Test
    fun `toEntity sets updatedAt to the ingestion run's own timestamp, not the source's meta-updatedAt`() {
        val ingestedAt = LocalDateTime.of(2026, 7, 5, 12, 0, 0)
        val product = productDto(meta = DummyJsonMetaDto(createdAt = "2024-01-15T09:30:00.000Z", updatedAt = "2020-01-01T00:00:00.000Z"))
            .toEntity(category, brand, AvailabilityStatus.IN_STOCK, ingestedAt)
        assertEquals(ingestedAt, product.updatedAt)
    }

    @Test
    fun `review toEntity converts int rating to byte and parses the review date`() {
        val product = productDto().toEntity(category, brand, AvailabilityStatus.IN_STOCK, LocalDateTime.now())
        val review = DummyJsonReviewDto(rating = 5, comment = "Great", date = "2024-02-01T00:00:00.000Z").toEntity(product)
        assertEquals(5.toByte(), review.rating)
        assertEquals(LocalDateTime.of(2024, 2, 1, 0, 0, 0), review.reviewDate)
    }

    @Test
    fun `image url toImageEntity converts int sortOrder to short`() {
        val product = productDto().toEntity(category, brand, AvailabilityStatus.IN_STOCK, LocalDateTime.now())
        val image = "https://example.com/img.png".toImageEntity(product, sortOrder = 3)
        assertEquals(3.toShort(), image.sortOrder)
        assertEquals("https://example.com/img.png", image.url)
    }

    @Test
    fun `linkTag builds the composite key from both ids`() {
        val product = productDto().toEntity(category, brand, AvailabilityStatus.IN_STOCK, LocalDateTime.now())
        val tag = Tag(id = 7, name = "featured")
        val link = product.linkTag(tag)
        assertEquals(product.id, link.id.productId)
        assertEquals(7, link.id.tagId)
    }

    @Test
    fun `toDocument flattens category to its slug and brand to its name`() {
        val product = productDto().toEntity(category, brand, AvailabilityStatus.IN_STOCK, LocalDateTime.now())
        val document = product.toDocument(listOf("featured", "sale"))
        assertEquals("1", document.id)
        assertEquals("smartphones", document.category)
        assertEquals("Apple", document.brand)
        assertEquals(listOf("featured", "sale"), document.tags)
    }

    @Test
    fun `toDocument converts BigDecimal money fields to double`() {
        val product = productDto(price = 19.99, discountPercentage = 12.5, rating = 4.5)
            .toEntity(category, brand, AvailabilityStatus.IN_STOCK, LocalDateTime.now())
        val document = product.toDocument(emptyList())
        assertEquals(19.99, document.price)
        assertEquals(12.5, document.discountPercentage)
        assertEquals(4.5, document.rating)
    }

    @Test
    fun `toDocument brand is null when the product has none`() {
        val product = productDto().toEntity(category, brand = null, status = AvailabilityStatus.IN_STOCK, ingestedAt = LocalDateTime.now())
        assertNull(product.toDocument(emptyList()).brand)
    }
}
