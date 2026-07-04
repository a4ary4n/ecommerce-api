package dev.aryan.ecommerceapi.ingestion

import dev.aryan.ecommerceapi.ingestion.client.DummyJsonClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

// Step 1 of 2: dummyjson -> MySQL. Independently triggerable from the ES reindex step
// (ProductSearchIndexService), which only ever reads MySQL - never a parallel dummyjson fetch.
@Service
class ProductCatalogSyncService(
    private val dummyJsonClient: DummyJsonClient,
    private val catalogWriter: ProductCatalogWriter,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun sync() = coroutineScope {
        // two independent HTTP calls - fetched concurrently
        val categoriesDeferred = async { dummyJsonClient.fetchCategories() }
        val productsDeferred = async { dummyJsonClient.fetchAllProducts() }
        val categories = categoriesDeferred.await()
        val response = productsDeferred.await()

        if (response.products.size != response.total) {
            log.warn("dummyjson reported total={} but returned {} products", response.total, response.products.size)
        }

        withContext(Dispatchers.IO) { catalogWriter.reload(categories, response.products) }
    }
}
