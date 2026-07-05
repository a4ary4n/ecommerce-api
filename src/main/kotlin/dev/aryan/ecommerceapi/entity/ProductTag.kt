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

/**
 * Composite primary key for [ProductTag] - the `(product_id, tag_id)` pair from the
 * `product_tags` join table.
 *
 * A `data class` deliberately, unlike the JPA entities in this package: unlike an
 * `@Entity`, this has no identity of its own, no lazy associations, and no Hibernate
 * proxying, so structural `equals`/`hashCode` is both safe and exactly what the JPA
 * spec requires (Hibernate uses this object itself as a lookup/cache key).
 */
@Embeddable
data class ProductTagId(
    @Column(name = "product_id")
    var productId: Int = 0,

    @Column(name = "tag_id")
    var tagId: Int = 0,
) : Serializable

/**
 * The many-to-many link row between [Product] and [Tag].
 *
 * Modeled as an explicit entity with `@EmbeddedId` (see [ProductTagId]) rather than an
 * implicit `@ManyToMany` join table, so the composite key stays visible instead of
 * hidden behind Hibernate-managed magic.
 *
 * [product] and [tag] are annotated `@MapsId` - each maps to the corresponding
 * component of [id] rather than an independent column, so setting one keeps the
 * embedded id's component in sync automatically instead of requiring both to be set
 * redundantly.
 */
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
