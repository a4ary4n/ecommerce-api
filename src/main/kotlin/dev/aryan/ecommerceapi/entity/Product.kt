package dev.aryan.ecommerceapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "products")
class Product(
    // no @GeneratedValue: reuses dummyjson's own product id as the PK
    @Id
    var id: Int,

    @Column(nullable = false, length = 255)
    var title: String,

    // TEXT in MySQL reports as JDBC LONGVARCHAR; @Lob would expect LONGTEXT and fail validation
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    var brand: Brand? = null,

    @Column(unique = true, length = 50)
    var sku: String? = null,

    @Column(nullable = false, precision = 10, scale = 2)
    var price: BigDecimal,

    @Column(precision = 5, scale = 2)
    var discountPercentage: BigDecimal? = null,

    @Column(precision = 3, scale = 2)
    var rating: BigDecimal? = null,

    var stock: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var availabilityStatus: AvailabilityStatus,

    @Column(precision = 8, scale = 2)
    var weight: BigDecimal? = null,

    @Column(precision = 8, scale = 2)
    var width: BigDecimal? = null,

    @Column(precision = 8, scale = 2)
    var height: BigDecimal? = null,

    @Column(precision = 8, scale = 2)
    var depth: BigDecimal? = null,

    @Column(length = 255)
    var warrantyInformation: String? = null,

    @Column(length = 255)
    var shippingInformation: String? = null,

    @Column(length = 255)
    var returnPolicy: String? = null,

    var minimumOrderQuantity: Int? = null,

    @Column(length = 500)
    var thumbnail: String? = null,

    @Column(length = 50)
    var barcode: String? = null,

    @Column(length = 500)
    var qrCode: String? = null,

    var createdAt: LocalDateTime? = null,

    var updatedAt: LocalDateTime? = null,
)
