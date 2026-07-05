package dev.aryan.ecommerceapi.repository

import dev.aryan.ecommerceapi.entity.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/** JPA repository for [Product]. */
interface ProductRepository : JpaRepository<Product, Int> {

    /**
     * All products with [Product.category]/[Product.brand] eagerly joined, in one query -
     * avoids N+1 across the full catalog when building Elasticsearch documents in
     * [dev.aryan.ecommerceapi.ingestion.ProductCatalogReader].
     *
     * `LEFT JOIN FETCH` on `brand` specifically, since it's nullable - an inner
     * `JOIN FETCH` would silently drop every brandless product from the result.
     */
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
