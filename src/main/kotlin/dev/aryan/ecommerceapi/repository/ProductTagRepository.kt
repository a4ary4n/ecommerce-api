package dev.aryan.ecommerceapi.repository

import dev.aryan.ecommerceapi.entity.ProductTag
import dev.aryan.ecommerceapi.entity.ProductTagId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/** JPA repository for [ProductTag]. */
interface ProductTagRepository : JpaRepository<ProductTag, ProductTagId> {

    /**
     * Tag names for a batch of products in one query, grouped by product id by the caller -
     * used both for the bulk Elasticsearch read-back (194 products at once) and for a
     * single product's detail view (a one-element [productIds]).
     */
    @Query(
        """
        SELECT pt.id.productId AS productId, t.name AS tagName
        FROM ProductTag pt JOIN pt.tag t
        WHERE pt.id.productId IN :productIds
        """
    )
    fun findTagNamesForProducts(@Param("productIds") productIds: Collection<Int>): List<ProductTagNameView>
}

/** Projection for [ProductTagRepository.findTagNamesForProducts]'s `productId`/`tagName` pairs. */
interface ProductTagNameView {
    fun getProductId(): Int
    fun getTagName(): String
}
