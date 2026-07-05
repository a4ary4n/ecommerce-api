package dev.aryan.ecommerceapi.web

import dev.aryan.ecommerceapi.entity.AvailabilityStatus
import dev.aryan.ecommerceapi.entity.Brand
import dev.aryan.ecommerceapi.entity.Category
import dev.aryan.ecommerceapi.entity.Product
import dev.aryan.ecommerceapi.entity.ProductImage
import dev.aryan.ecommerceapi.entity.Review
import dev.aryan.ecommerceapi.search.ProductDocument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Unit tests for the entity/document -> REST response DTO mapping extension functions - pure
 * functions, no Spring context needed. Confirms the list view ([Product.toDocument]-shaped)
 * and detail view ([Product.toDetailResponse]) genuinely differ in the fields they expose,
 * mirroring the MySQL/Elasticsearch split at the API layer.
 */
class WebMappingsTest {

    private val category = Category(id = 1, slug = "smartphones", name = "Smartphones")
    private val brand = Brand(id = 1, name = "Apple")

    private fun product(brand: Brand? = this.brand) = Product(
        id = 1,
        title = "iPhone",
        description = "A phone",
        category = category,
        brand = brand,
        price = BigDecimal("999.99"),
        availabilityStatus = AvailabilityStatus.IN_STOCK,
        warrantyInformation = "1 year",
        shippingInformation = "Ships in 1 day",
        returnPolicy = "30 days",
        barcode = "123456",
        qrCode = "https://example.com/qr.png",
        createdAt = LocalDateTime.of(2024, 1, 1, 0, 0),
        updatedAt = LocalDateTime.of(2024, 2, 1, 0, 0),
    )

    @Test
    fun `category toSummary maps all three fields`() {
        val summary = category.toSummary()
        assertEquals(1, summary.id)
        assertEquals("smartphones", summary.slug)
        assertEquals("Smartphones", summary.name)
    }

    @Test
    fun `review toResponse maps every field`() {
        val review = Review(
            product = product(),
            rating = 5,
            comment = "Great",
            reviewerName = "Alice",
            reviewerEmail = "alice@example.com",
            reviewDate = LocalDateTime.of(2024, 3, 1, 0, 0),
        )
        val response = review.toResponse()
        assertEquals(5.toByte(), response.rating)
        assertEquals("Great", response.comment)
        assertEquals("Alice", response.reviewerName)
        assertEquals("alice@example.com", response.reviewerEmail)
        assertEquals(LocalDateTime.of(2024, 3, 1, 0, 0), response.reviewDate)
    }

    @Test
    fun `product image toResponse maps url and sortOrder`() {
        val image = ProductImage(product = product(), url = "https://example.com/1.png", sortOrder = 2)
        val response = image.toResponse()
        assertEquals("https://example.com/1.png", response.url)
        assertEquals(2.toShort(), response.sortOrder)
    }

    @Test
    fun `productDocument toSummaryResponse converts the string id back to int`() {
        val document = ProductDocument(
            id = "42",
            title = "iPhone",
            description = "A phone",
            category = "smartphones",
            brand = "Apple",
            tags = listOf("featured"),
            price = 999.99,
            discountPercentage = 10.0,
            rating = 4.5,
            stock = 10,
            availabilityStatus = "IN_STOCK",
            thumbnail = "https://example.com/thumb.png",
            createdAt = LocalDateTime.of(2024, 1, 1, 0, 0),
        )
        val response = document.toSummaryResponse()
        assertEquals(42, response.id)
        assertEquals("smartphones", response.category)
        assertEquals("Apple", response.brand)
        assertEquals(listOf("featured"), response.tags)
    }

    @Test
    fun `product toDetailResponse includes fields the list view deliberately omits`() {
        val response = product().toDetailResponse(reviews = emptyList(), images = emptyList(), tags = emptyList())
        assertEquals("1 year", response.warrantyInformation)
        assertEquals("Ships in 1 day", response.shippingInformation)
        assertEquals("30 days", response.returnPolicy)
        assertEquals("123456", response.barcode)
        assertEquals("https://example.com/qr.png", response.qrCode)
        assertEquals(LocalDateTime.of(2024, 2, 1, 0, 0), response.updatedAt)
    }

    @Test
    fun `product toDetailResponse nests category as a summary and brand as just its name`() {
        val response = product().toDetailResponse(emptyList(), emptyList(), emptyList())
        assertEquals("smartphones", response.category.slug)
        assertEquals("Apple", response.brand)
    }

    @Test
    fun `product toDetailResponse brand is null when the product has none`() {
        val response = product(brand = null).toDetailResponse(emptyList(), emptyList(), emptyList())
        assertNull(response.brand)
    }

    @Test
    fun `product toDetailResponse maps reviews, images, and tags through their own mappers`() {
        val review = Review(product = product(), rating = 4, comment = "Good")
        val image = ProductImage(product = product(), url = "https://example.com/1.png", sortOrder = 1)
        val response = product().toDetailResponse(
            reviews = listOf(review),
            images = listOf(image),
            tags = listOf("featured"),
        )
        assertEquals(1, response.reviews.size)
        assertEquals(4.toByte(), response.reviews[0].rating)
        assertEquals(1, response.images.size)
        assertEquals("https://example.com/1.png", response.images[0].url)
        assertEquals(listOf("featured"), response.tags)
    }
}
