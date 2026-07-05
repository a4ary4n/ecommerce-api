package dev.aryan.ecommerceapi.ingestion

import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Boot-time trigger for the dummyjson -> MySQL sync flow ([ProductCatalogSyncService]).
 * Only runs when `app.ingest.mysql.enabled=true` - off by default locally, on in the
 * docker-compose `app` service so the catalog self-populates on every container boot.
 */
@Component
@ConditionalOnProperty(name = ["app.ingest.mysql.enabled"], havingValue = "true")
@Order(1) // runs before ProductSearchIndexRunner if both are enabled in the same boot
class ProductCatalogSyncRunner(private val catalogSyncService: ProductCatalogSyncService) : CommandLineRunner {
    override fun run(vararg args: String) = runBlocking { catalogSyncService.sync() }
}
