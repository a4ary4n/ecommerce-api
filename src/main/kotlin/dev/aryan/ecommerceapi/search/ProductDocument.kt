package dev.aryan.ecommerceapi.search

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.annotations.InnerField
import org.springframework.data.elasticsearch.annotations.MultiField
import java.time.LocalDateTime

// createIndex=false: ElasticsearchIndexer owns index create/delete explicitly (drop+recreate
// every sync run) - Spring Data's own default (createIndex=true) auto-creates an empty index
// the moment ProductSearchRepository is instantiated, unconditionally on every app boot,
// which would touch ES even when only the mysql-sync flow is enabled.
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
