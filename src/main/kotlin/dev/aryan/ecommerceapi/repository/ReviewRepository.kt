package dev.aryan.ecommerceapi.repository

import dev.aryan.ecommerceapi.entity.Review
import org.springframework.data.jpa.repository.JpaRepository

/** JPA repository for [Review]. */
interface ReviewRepository : JpaRepository<Review, Long> {
    /** All reviews for one product, used to assemble `GET /products/{id}`'s full detail. */
    fun findByProductId(productId: Int): List<Review>
}
