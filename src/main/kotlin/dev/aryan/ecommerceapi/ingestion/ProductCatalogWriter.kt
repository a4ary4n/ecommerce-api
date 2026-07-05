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

/**
 * Wipes and reloads the entire MySQL catalog from dummyjson data - the write half of the
 * dummyjson -> MySQL ingestion flow. Safe to run repeatedly (every container boot):
 * every call deletes all 7 tables in FK-safe order and rebuilds from scratch.
 */
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

    /**
     * Wipes all 7 tables, seeds categories from [categories], then persists [products] and
     * their reviews/images/tags. Brands and tags are resolved-or-created via an in-memory
     * cache for the duration of this call (safe since the tables were just wiped - no need
     * to check for pre-existing rows). Products with an unknown category slug or an
     * unrecognized `availabilityStatus` are skipped and logged, not treated as fatal.
     */
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

    /** Deletes all 7 tables in FK-safe (children-first) order. */
    private fun wipeAll() {
        productTagRepository.deleteAllInBatch()
        reviewRepository.deleteAllInBatch()
        productImageRepository.deleteAllInBatch()
        productRepository.deleteAllInBatch()
        categoryRepository.deleteAllInBatch()
        brandRepository.deleteAllInBatch()
        tagRepository.deleteAllInBatch()
    }

    /**
     * Persists [categories] fresh (table was just wiped) and returns a slug -> [Category]
     * lookup for resolving each product's category during the main load loop.
     */
    private fun seedCategories(categories: List<DummyJsonCategoryDto>): Map<String, Category> =
        categoryRepository.saveAll(categories.map { Category(slug = it.slug, name = it.name) })
            .associateBy { it.slug }
}
