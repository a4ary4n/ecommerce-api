package dev.aryan.ecommerceapi.repository

import dev.aryan.ecommerceapi.entity.ProductImage
import org.springframework.data.jpa.repository.JpaRepository

interface ProductImageRepository : JpaRepository<ProductImage, Long> {
    fun findByProductIdOrderBySortOrderAsc(productId: Int): List<ProductImage>
}
