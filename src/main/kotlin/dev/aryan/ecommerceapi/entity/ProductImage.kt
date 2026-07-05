package dev.aryan.ecommerceapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * A single product image URL, mirroring one row of the `product_images` table.
 *
 * @property url Image location; display-only, never queried, hence not part of the
 *   flat [dev.aryan.ecommerceapi.search.ProductDocument].
 * @property sortOrder Display order among a product's images; `Short` (not `Int`) to
 *   match the MySQL `SMALLINT` column.
 */
@Entity
@Table(name = "product_images")
class ProductImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @Column(nullable = false, length = 500)
    var url: String,

    var sortOrder: Short? = null,
)
