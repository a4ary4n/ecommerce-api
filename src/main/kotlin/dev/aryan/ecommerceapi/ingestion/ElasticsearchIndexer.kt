package dev.aryan.ecommerceapi.ingestion

import dev.aryan.ecommerceapi.search.ProductDocument
import dev.aryan.ecommerceapi.search.ProductSearchRepository
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

    /**
     * Drops and recreates the `products` index (mapping derived from [ProductDocument]'s
     * annotations), then bulk-indexes [documents]. Always a full rebuild, never an
     * incremental update - mirrors the MySQL side's wipe-and-reload idempotency.
     */
    fun reindex(documents: List<ProductDocument>) {
        val indexOps = elasticsearchOperations.indexOps(ProductDocument::class.java)
        if (indexOps.exists()) {
            indexOps.delete()
        }
        indexOps.create()
        indexOps.putMapping()
        productSearchRepository.saveAll(documents)
    }
}
