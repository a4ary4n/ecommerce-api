package dev.aryan.ecommerceapi.ingestion

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

// Step 2 of 2: MySQL -> Elasticsearch. Reads whatever is currently in MySQL and rebuilds
// the search index from it - no network call to dummyjson, MySQL is the only dependency.
// Purely sequential (one MySQL read, one ES write) - no independent work to run
// concurrently, so no coroutines here (unlike ProductCatalogSyncService).
@Service
class ProductSearchIndexService(
    private val catalogReader: ProductCatalogReader,
    private val elasticsearchIndexer: ElasticsearchIndexer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun reindex() {
        val documents = catalogReader.readAllAsDocuments()
        elasticsearchIndexer.reindex(documents)
        log.info("search index rebuilt: {} documents indexed", documents.size)
    }
}
