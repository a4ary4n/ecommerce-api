package dev.aryan.ecommerceapi.web

import dev.aryan.ecommerceapi.service.ProductDetailService
import dev.aryan.ecommerceapi.service.ProductSearchService
import dev.aryan.ecommerceapi.web.dto.PageResponse
import dev.aryan.ecommerceapi.web.dto.ProductDetailResponse
import dev.aryan.ecommerceapi.web.dto.ProductSearchParams
import dev.aryan.ecommerceapi.web.dto.ProductSummaryResponse
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
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<ProductSummaryResponse> =
        productSearchService.search(ProductSearchParams(query, category, page, size))

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): ResponseEntity<ProductDetailResponse> =
        productDetailService.getById(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    // PageRequest.of(page, size) throws IllegalArgumentException for page < 0 or size < 1
    // (e.g. ?size=0) - a plain IllegalArgumentException isn't auto-mapped to 400 by Spring
    // MVC by default, so without this it surfaces as an uncaught 500.
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleInvalidRequest(ex: IllegalArgumentException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.badRequest().body(mapOf("error" to ex.message))
}
