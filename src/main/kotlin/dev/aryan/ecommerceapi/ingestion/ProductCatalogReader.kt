package dev.aryan.ecommerceapi.ingestion

import dev.aryan.ecommerceapi.repository.ProductRepository
import dev.aryan.ecommerceapi.repository.ProductTagRepository
import dev.aryan.ecommerceapi.search.ProductDocument
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Reads the full product catalog back from MySQL and assembles it into flat
 * [ProductDocument]s, ready for [ElasticsearchIndexer] to bulk-load - the "MySQL is the
 * source of truth" half of the search-index-rebuild flow. Never touches dummyjson.
 */
@Component
class ProductCatalogReader(
    private val productRepository: ProductRepository,
    private val productTagRepository: ProductTagRepository,
) {

    /**
     * All products as [ProductDocument]s. Exactly two queries regardless of catalog size -
     * one JOIN-FETCH read for products+category+brand
     * ([ProductRepository.findAllWithCategoryAndBrand]), one batched tag lookup
     * ([ProductTagRepository.findTagNamesForProducts]) - avoiding N+1 across the whole
     * catalog. `readOnly` keeps the Hibernate session open long enough for both lazy
     * associations to resolve, despite `open-in-view: false`.
     */
    @Transactional(readOnly = true)
    fun readAllAsDocuments(): List<ProductDocument> {
        val products = productRepository.findAllWithCategoryAndBrand()

        val tagsByProductId = productTagRepository
            .findTagNamesForProducts(products.map { it.id })
            .groupBy({ it.getProductId() }, { it.getTagName() })

        return products.map { product ->
            product.toDocument(tagsByProductId[product.id].orEmpty())
        }
    }
}
