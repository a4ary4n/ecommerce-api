package dev.aryan.ecommerceapi.ingestion

import dev.aryan.ecommerceapi.search.ProductDocument
import dev.aryan.ecommerceapi.search.ProductSearchRepository
import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.stereotype.Component

/**
 * Owns the `products` Elasticsearch index's lifecycle explicitly - creation, mapping, and
 * bulk loading all happen here rather than being left to Spring Data Elasticsearch's own
 * defaults (see [ProductDocument]'s `createIndex=false` for why that matters).
 */
@Component
class ElasticsearchIndexer(
    private val elasticsearchOperations: ElasticsearchOperations,
    private val productSearchRepository: ProductSearchRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Drops and recreates the `products` index (mapping derived from [ProductDocument]'s
     * annotations), then bulk-indexes [documents]. Always a full rebuild, never an
     * incremental update - mirrors the MySQL side's wipe-and-reload idempotency.
     */
    fun reindex(documents: List<ProductDocument>) {
        val indexOps = elasticsearchOperations.indexOps(ProductDocument::class.java)
        if (indexOps.exists()) {
            log.info("dropping existing 'products' index")
            indexOps.delete()
        }
        indexOps.create()
        indexOps.putMapping()
        log.info("'products' index (re)created, bulk-indexing {} documents", documents.size)
        productSearchRepository.saveAll(documents)
    }
}
