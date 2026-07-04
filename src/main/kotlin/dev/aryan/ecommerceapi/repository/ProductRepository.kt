package dev.aryan.ecommerceapi.repository

import dev.aryan.ecommerceapi.entity.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ProductRepository : JpaRepository<Product, Int> {

    @Query(
        """
        SELECT p FROM Product p
        JOIN FETCH p.category
        LEFT JOIN FETCH p.brand
        ORDER BY p.id
        """
    )
    fun findAllWithCategoryAndBrand(): List<Product>
}
