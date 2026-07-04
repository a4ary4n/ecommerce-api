package dev.aryan.ecommerceapi.web

import dev.aryan.ecommerceapi.service.ProductDetailService
import dev.aryan.ecommerceapi.web.dto.ProductDetailResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/products")
class ProductController(private val productDetailService: ProductDetailService) {

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): ResponseEntity<ProductDetailResponse> =
        productDetailService.getById(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

}
