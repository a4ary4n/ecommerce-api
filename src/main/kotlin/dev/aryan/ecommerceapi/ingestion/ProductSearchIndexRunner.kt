package dev.aryan.ecommerceapi.ingestion

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Boot-time trigger for the MySQL -> Elasticsearch reindex flow ([ProductSearchIndexService]).
 * Only runs when `app.ingest.elasticsearch.enabled=true` - off by default locally, on in
 * the docker-compose `app` service. Never touches dummyjson, independent of
 * [ProductCatalogSyncRunner].
 */
@Component
@ConditionalOnProperty(name = ["app.ingest.elasticsearch.enabled"], havingValue = "true")
@Order(2) // runs after ProductCatalogSyncRunner if both are enabled in the same boot
class ProductSearchIndexRunner(private val searchIndexService: ProductSearchIndexService) : CommandLineRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String) {
        log.info("app.ingest.elasticsearch.enabled=true - triggering MySQL -> Elasticsearch reindex")
        searchIndexService.reindex()
    }
}
