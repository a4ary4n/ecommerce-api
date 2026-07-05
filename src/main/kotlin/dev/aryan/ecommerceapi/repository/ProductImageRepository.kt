package dev.aryan.ecommerceapi.repository

import dev.aryan.ecommerceapi.entity.ProductImage
import org.springframework.data.jpa.repository.JpaRepository

/** JPA repository for [ProductImage]. */
interface ProductImageRepository : JpaRepository<ProductImage, Long> {
    /**
     * All images for one product, in display order. Used to assemble `GET /products/{id}`'s
     * full detail.
     */
    fun findByProductIdOrderBySortOrderAsc(productId: Int): List<ProductImage>
}
