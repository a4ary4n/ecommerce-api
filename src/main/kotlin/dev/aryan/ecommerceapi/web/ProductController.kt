package dev.aryan.ecommerceapi.web

import dev.aryan.ecommerceapi.service.ProductDetailService
import dev.aryan.ecommerceapi.service.ProductSearchService
import dev.aryan.ecommerceapi.web.dto.PageResponse
import dev.aryan.ecommerceapi.web.dto.ProductDetailResponse
import dev.aryan.ecommerceapi.web.dto.ProductSearchParams
import dev.aryan.ecommerceapi.web.dto.ProductSummaryResponse
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** `GET /products` (list/search/filter, Elasticsearch-backed) and `GET /products/{id}` (full detail, MySQL-backed). */
@RestController
@RequestMapping("/products")
class ProductController(
    private val productSearchService: ProductSearchService,
    private val productDetailService: ProductDetailService,
) {

    /**
     * List, full-text search, and filter products. `query`/`category`/`page`/`size` are the
     * assignment-required baseline; `brand`/`minPrice`/`maxPrice`/`sort`/`minRating`/
     * `inStock` are additional search capabilities layered on top (see [ProductSearchParams]
     * and [ProductSearchQueryBuilder][dev.aryan.ecommerceapi.service.ProductSearchQueryBuilder]
     * for exactly how each param behaves).
     */
    @GetMapping
    fun search(
        @RequestParam query: String?,
        @RequestParam category: String?,
        @RequestParam brand: String?,
        @RequestParam @DecimalMin("0.0") minPrice: Double?,
        @RequestParam @DecimalMin("0.0") maxPrice: Double?,
        @RequestParam @DecimalMin("0.0") @DecimalMax("5.0") minRating: Double?,
        @RequestParam inStock: Boolean?,
        @RequestParam sort: String?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): PageResponse<ProductSummaryResponse> =
        productSearchService.search(
            ProductSearchParams(query, category, brand, minPrice, maxPrice, minRating, inStock, sort, page, size)
        )

    /** Full product detail, or 404 if [id] doesn't exist. */
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): ResponseEntity<ProductDetailResponse> =
        productDetailService.getById(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    /**
     * Maps cross-field business-rule violations to 400. `page`/`size`'s simple structural
     * bounds (`page >= 0`, `1 <= size <= 100`) are declared via `@Min`/`@Max` above instead -
     * Spring MVC (6.1+) auto-maps the resulting `HandlerMethodValidationException` to 400,
     * no handler needed for those. What `@Min`/`@Max` can't express is `page*size+size <=
     * 10_000` (Elasticsearch's `index.max_result_window`) or an unrecognized `sort` value -
     * both cross-field/business rules enforced via `require()`/`throw` in
     * [ProductSearchQueryBuilder][dev.aryan.ecommerceapi.service.ProductSearchQueryBuilder],
     * which throws a plain [IllegalArgumentException] - not auto-mapped to 400 by Spring MVC
     * by default, hence this handler.
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleInvalidRequest(ex: IllegalArgumentException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.badRequest().body(mapOf("error" to ex.message))
}
