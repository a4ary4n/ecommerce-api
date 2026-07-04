package dev.aryan.ecommerceapi.ingestion.client

import dev.aryan.ecommerceapi.ingestion.dto.DummyJsonCategoryDto
import dev.aryan.ecommerceapi.ingestion.dto.DummyJsonProductListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class DummyJsonClient(private val dummyJsonRestClient: RestClient) {

    // limit=0 is dummyjson's documented way to return every item in one call.
    // RestClient is blocking, so the actual HTTP call is offloaded to Dispatchers.IO.
    suspend fun fetchAllProducts(): DummyJsonProductListResponse = withContext(Dispatchers.IO) {
        dummyJsonRestClient.get()
            .uri("/products?limit=0")
            .retrieve()
            .body(DummyJsonProductListResponse::class.java)!!
    }

    // raw JSON array, no envelope - the only source of proper category display names
    suspend fun fetchCategories(): List<DummyJsonCategoryDto> = withContext(Dispatchers.IO) {
        dummyJsonRestClient.get()
            .uri("/products/categories")
            .retrieve()
            .body(object : ParameterizedTypeReference<List<DummyJsonCategoryDto>>() {})!!
    }
}
