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

@RestController
@RequestMapping("/products")
class ProductController(
    private val productSearchService: ProductSearchService,
    private val productDetailService: ProductDetailService,
) {

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

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): ResponseEntity<ProductDetailResponse> =
        productDetailService.getById(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    // page/size structural bounds (page >= 0, 1 <= size <= 100) are declared above via
    // @Min/@Max - Spring MVC (6.1+) maps a HandlerMethodValidationException from those
    // automatically to 400, no handler needed here for that case.
    //
    // What @Min/@Max can't express: page*size + size <= 10_000 (Elasticsearch's
    // index.max_result_window) is a cross-field business rule, not a simple bound on one
    // parameter - ProductSearchQueryBuilder enforces it via require(), which throws a plain
    // IllegalArgumentException. That type isn't auto-mapped to 400 by Spring MVC by default,
    // so this handler stays for that one case specifically.
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleInvalidRequest(ex: IllegalArgumentException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.badRequest().body(mapOf("error" to ex.message))
}
