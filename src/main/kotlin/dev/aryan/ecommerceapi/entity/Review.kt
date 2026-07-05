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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * A single product review, mirroring one row of the `reviews` table.
 *
 * Reviewer identity is intentionally not normalized into its own table - the source
 * data has no stable user id, so inventing one would fabricate a relationship that
 * doesn't exist.
 *
 * @property rating 1-5 star rating; `Byte` (not `Int`) to match the MySQL `TINYINT` column.
 * @property comment Mapped `@JdbcTypeCode(SqlTypes.LONGVARCHAR)`, not `@Lob` - the MySQL
 *   column is `TEXT`, not `LONGTEXT` (see [Product.description] for the same trap).
 * @property reviewDate The source's own review timestamp, never fabricated.
 */
@Entity
@Table(name = "reviews")
class Review(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @Column(nullable = false)
    var rating: Byte,

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    var comment: String? = null,

    @Column(length = 150)
    var reviewerName: String? = null,

    @Column(length = 255)
    var reviewerEmail: String? = null,

    var reviewDate: LocalDateTime? = null,
)
