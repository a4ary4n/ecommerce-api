package dev.aryan.ecommerceapi.repository

import dev.aryan.ecommerceapi.entity.ProductTag
import dev.aryan.ecommerceapi.entity.ProductTagId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductTagRepository : JpaRepository<ProductTag, ProductTagId> {

    @Query(
        """
        SELECT pt.id.productId AS productId, t.name AS tagName
        FROM ProductTag pt JOIN pt.tag t
        WHERE pt.id.productId IN :productIds
        """
    )
    fun findTagNamesForProducts(@Param("productIds") productIds: Collection<Int>): List<ProductTagNameView>
}

interface ProductTagNameView {
    fun getProductId(): Int
    fun getTagName(): String
}
