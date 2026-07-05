package dev.aryan.ecommerceapi.ingestion.client

import dev.aryan.ecommerceapi.ingestion.dto.DummyJsonCategoryDto
import dev.aryan.ecommerceapi.ingestion.dto.DummyJsonProductListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/** Thin HTTP client for the two dummyjson endpoints ingestion needs. */
@Component
class DummyJsonClient(private val dummyJsonRestClient: RestClient) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * All products in one call, via `limit=0` (dummyjson's documented way to return every
     * item rather than the default-30 page). `RestClient` is blocking, so the call is
     * offloaded to [Dispatchers.IO].
     */
    suspend fun fetchAllProducts(): DummyJsonProductListResponse = withContext(Dispatchers.IO) {
        log.debug("fetching all products from dummyjson (GET /products?limit=0)")
        val response = dummyJsonRestClient.get()
            .uri("/products?limit=0")
            .retrieve()
            .body(DummyJsonProductListResponse::class.java)!!
        log.debug("dummyjson returned {} of {} reported products", response.products.size, response.total)
        response
    }

    /**
     * All categories (a raw JSON array, no envelope) - the only source of proper category
     * display names; a product's own `category` field is just the slug.
     */
    suspend fun fetchCategories(): List<DummyJsonCategoryDto> = withContext(Dispatchers.IO) {
        log.debug("fetching categories from dummyjson (GET /products/categories)")
        val categories = dummyJsonRestClient.get()
            .uri("/products/categories")
            .retrieve()
            .body(object : ParameterizedTypeReference<List<DummyJsonCategoryDto>>() {})!!
        log.debug("dummyjson returned {} categories", categories.size)
        categories
    }
}
