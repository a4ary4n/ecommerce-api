package dev.aryan.ecommerceapi.service

import dev.aryan.ecommerceapi.search.ProductDocument
import dev.aryan.ecommerceapi.search.ProductSearchQueryBuilder
import dev.aryan.ecommerceapi.web.dto.PageResponse
import dev.aryan.ecommerceapi.web.dto.ProductSearchParams
import dev.aryan.ecommerceapi.web.dto.ProductSummaryResponse
import dev.aryan.ecommerceapi.web.toSummaryResponse
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.stereotype.Service

/** Backs `GET /products`'s list/search/filter behavior. */
@Service
class ProductSearchService(private val elasticsearchOperations: ElasticsearchOperations) {

    /**
     * Runs the query [ProductSearchQueryBuilder] builds from [params] and maps the
     * resulting [ProductDocument]s into a paginated response.
     */
    fun search(params: ProductSearchParams): PageResponse<ProductSummaryResponse> {
        val nativeQuery = ProductSearchQueryBuilder.build(params)
        val hits = elasticsearchOperations.search(nativeQuery, ProductDocument::class.java)

        val content = hits.searchHits.map { it.content.toSummaryResponse() }
        val totalPages = if (params.size > 0) ((hits.totalHits + params.size - 1) / params.size).toInt() else 0

        return PageResponse(
            content = content,
            page = params.page,
            size = params.size,
            totalElements = hits.totalHits,
            totalPages = totalPages,
        )
    }
}
