package dev.aryan.ecommerceapi.ingestion

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.ingest.elasticsearch.enabled"], havingValue = "true")
@Order(2) // runs after ProductCatalogSyncRunner if both are enabled in the same boot
class ProductSearchIndexRunner(private val searchIndexService: ProductSearchIndexService) : CommandLineRunner {
    override fun run(vararg args: String) = searchIndexService.reindex()
}
