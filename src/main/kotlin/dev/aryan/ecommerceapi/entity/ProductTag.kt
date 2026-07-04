package dev.aryan.ecommerceapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import java.io.Serializable

@Embeddable
data class ProductTagId(
    @Column(name = "product_id")
    var productId: Int = 0,

    @Column(name = "tag_id")
    var tagId: Int = 0,
) : Serializable

@Entity
@Table(name = "product_tags")
class ProductTag(
    @EmbeddedId
    var id: ProductTagId,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    var product: Product,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id")
    var tag: Tag,
)
