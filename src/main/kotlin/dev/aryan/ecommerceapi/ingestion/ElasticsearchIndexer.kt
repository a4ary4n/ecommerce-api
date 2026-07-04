package dev.aryan.ecommerceapi.ingestion

import dev.aryan.ecommerceapi.search.ProductDocument
import dev.aryan.ecommerceapi.search.ProductSearchRepository
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.stereotype.Component

@Component
class ElasticsearchIndexer(
    private val elasticsearchOperations: ElasticsearchOperations,
    private val productSearchRepository: ProductSearchRepository,
) {

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
