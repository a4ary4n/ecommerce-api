package dev.aryan.ecommerceapi.ingestion

import dev.aryan.ecommerceapi.entity.Brand
import dev.aryan.ecommerceapi.entity.Category
import dev.aryan.ecommerceapi.entity.ProductImage
import dev.aryan.ecommerceapi.entity.ProductTag
import dev.aryan.ecommerceapi.entity.Review
import dev.aryan.ecommerceapi.entity.Tag
import dev.aryan.ecommerceapi.ingestion.dto.DummyJsonCategoryDto
import dev.aryan.ecommerceapi.ingestion.dto.DummyJsonProductDto
import dev.aryan.ecommerceapi.repository.BrandRepository
import dev.aryan.ecommerceapi.repository.CategoryRepository
import dev.aryan.ecommerceapi.repository.ProductImageRepository
import dev.aryan.ecommerceapi.repository.ProductRepository
import dev.aryan.ecommerceapi.repository.ProductTagRepository
import dev.aryan.ecommerceapi.repository.ReviewRepository
import dev.aryan.ecommerceapi.repository.TagRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class ProductCatalogWriter(
    private val categoryRepository: CategoryRepository,
    private val brandRepository: BrandRepository,
    private val tagRepository: TagRepository,
    private val productRepository: ProductRepository,
    private val reviewRepository: ReviewRepository,
    private val productImageRepository: ProductImageRepository,
    private val productTagRepository: ProductTagRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun reload(categories: List<DummyJsonCategoryDto>, products: List<DummyJsonProductDto>) {
        wipeAll()
        val categoryBySlug = seedCategories(categories)
        val brandCache = mutableMapOf<String, Brand>()
        val tagCache = mutableMapOf<String, Tag>()
        val ingestedAt = LocalDateTime.now()

        val allReviews = mutableListOf<Review>()
        val allImages = mutableListOf<ProductImage>()
        val allProductTags = mutableListOf<ProductTag>()
        var skipped = 0

        products.forEach { dto ->
            val category = categoryBySlug[dto.category]
            val status = dto.availabilityStatus.toAvailabilityStatus()

            if (category == null) {
                log.warn("skipping product {} ({}): unknown category slug '{}'", dto.id, dto.title, dto.category)
                skipped++
                return@forEach
            }
            if (status == null) {
                log.warn("skipping product {} ({}): unrecognized availabilityStatus '{}'", dto.id, dto.title, dto.availabilityStatus)
                skipped++
                return@forEach
            }

            // dto.brand absent -> null here -> brand_id stored as NULL, no brand row touched
            val brand = dto.brand?.let { name -> brandCache.getOrPut(name) { brandRepository.save(Brand(name = name)) } }

            val savedProduct = productRepository.save(dto.toEntity(category, brand, status, ingestedAt))
            // always reference savedProduct below - Product.id is never null so save() goes
            // through entityManager.merge(), which returns a different managed instance

            dto.reviews.forEach { review -> allReviews += review.toEntity(savedProduct) }
            dto.images.forEachIndexed { index, url -> allImages += url.toImageEntity(savedProduct, index) }
            dto.tags.distinct().forEach { name ->
                val tag = tagCache.getOrPut(name) { tagRepository.save(Tag(name = name)) }
                allProductTags += savedProduct.linkTag(tag)
            }
        }

        reviewRepository.saveAll(allReviews)
        productImageRepository.saveAll(allImages)
        productTagRepository.saveAll(allProductTags)
        log.info("reload complete: {} persisted, {} skipped", products.size - skipped, skipped)
    }

    private fun wipeAll() {
        productTagRepository.deleteAllInBatch()
        reviewRepository.deleteAllInBatch()
        productImageRepository.deleteAllInBatch()
        productRepository.deleteAllInBatch()
        categoryRepository.deleteAllInBatch()
        brandRepository.deleteAllInBatch()
        tagRepository.deleteAllInBatch()
    }

    private fun seedCategories(categories: List<DummyJsonCategoryDto>): Map<String, Category> =
        categoryRepository.saveAll(categories.map { Category(slug = it.slug, name = it.name) })
            .associateBy { it.slug }
}
