package dev.aryan.ecommerceapi.search

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.annotations.InnerField
import org.springframework.data.elasticsearch.annotations.MultiField
import java.time.LocalDateTime

/**
 * The flat, denormalized Elasticsearch representation of a [dev.aryan.ecommerceapi.entity.Product]
 * - built by reading products back from MySQL after ingestion (never a parallel fetch from
 * dummyjson), so the two stores can never disagree. Backs `GET /products`'s search/list/filter
 * endpoint; full product detail (reviews, images, dimensions, etc.) is deliberately not
 * duplicated here, and is instead served from MySQL via `GET /products/{id}`.
 *
 * `createIndex=false`: [ElasticsearchIndexer][dev.aryan.ecommerceapi.ingestion.ElasticsearchIndexer]
 * owns index create/delete explicitly (drop+recreate every sync run) - Spring Data's own
 * default (`createIndex=true`) auto-creates an empty index the moment
 * [ProductSearchRepository] is instantiated, unconditionally on every app boot, which would
 * touch Elasticsearch even when only the MySQL-sync ingestion flow is enabled.
 *
 * @property id String form of [dev.aryan.ecommerceapi.entity.Product.id] - the conventional
 *   type for an Elasticsearch document `_id`.
 * @property category The category's *slug*, not its display name - the value `GET
 *   /products?category=` filters against, matching the categories table's natural key.
 * @property price Mapped `Double`, deliberately asymmetric with MySQL's `BigDecimal` - this
 *   is a derived, read-optimized index, not the source of truth, so float semantics here
 *   are conventional and fine. Same reasoning applies to [discountPercentage], [rating],
 *   and [stock].
 */
@Document(indexName = "products", createIndex = false)
data class ProductDocument(
    @Id
    val id: String,

    @MultiField(
        mainField = Field(type = FieldType.Text),
        otherFields = [InnerField(suffix = "keyword", type = FieldType.Keyword)],
    )
    val title: String,

    @Field(type = FieldType.Text)
    val description: String?,

    // stores the category SLUG (filter key), not the display name
    @Field(type = FieldType.Keyword)
    val category: String,

    @MultiField(
        mainField = Field(type = FieldType.Text),
        otherFields = [InnerField(suffix = "keyword", type = FieldType.Keyword)],
    )
    val brand: String?,

    @MultiField(
        mainField = Field(type = FieldType.Text),
        otherFields = [InnerField(suffix = "keyword", type = FieldType.Keyword)],
    )
    val tags: List<String>,

    @Field(type = FieldType.Double)
    val price: Double,

    @Field(type = FieldType.Double)
    val discountPercentage: Double?,

    @Field(type = FieldType.Double)
    val rating: Double?,

    @Field(type = FieldType.Integer)
    val stock: Int?,

    @Field(type = FieldType.Keyword)
    val availabilityStatus: String,

    @Field(type = FieldType.Keyword, index = false)
    val thumbnail: String?,

    // explicit format - without it Spring Data ES silently truncated LocalDateTime to date-only on write
    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second_millis])
    val createdAt: LocalDateTime?,
)
