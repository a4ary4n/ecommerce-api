package dev.aryan.ecommerceapi.ingestion.client

import dev.aryan.ecommerceapi.ingestion.dto.DummyJsonCategoryDto
import dev.aryan.ecommerceapi.ingestion.dto.DummyJsonProductListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/** Thin HTTP client for the two dummyjson endpoints ingestion needs. */
@Component
class DummyJsonClient(private val dummyJsonRestClient: RestClient) {

    /**
     * All products in one call, via `limit=0` (dummyjson's documented way to return every
     * item rather than the default-30 page). `RestClient` is blocking, so the call is
     * offloaded to [Dispatchers.IO].
     */
    suspend fun fetchAllProducts(): DummyJsonProductListResponse = withContext(Dispatchers.IO) {
        dummyJsonRestClient.get()
            .uri("/products?limit=0")
            .retrieve()
            .body(DummyJsonProductListResponse::class.java)!!
    }

    /**
     * All categories (a raw JSON array, no envelope) - the only source of proper category
     * display names; a product's own `category` field is just the slug.
     */
    suspend fun fetchCategories(): List<DummyJsonCategoryDto> = withContext(Dispatchers.IO) {
        dummyJsonRestClient.get()
            .uri("/products/categories")
            .retrieve()
            .body(object : ParameterizedTypeReference<List<DummyJsonCategoryDto>>() {})!!
    }
}
