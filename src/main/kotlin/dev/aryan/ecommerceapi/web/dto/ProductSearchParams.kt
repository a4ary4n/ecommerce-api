package dev.aryan.ecommerceapi.web.dto

/**
 * Parsed `GET /products` query params, passed to
 * [ProductSearchQueryBuilder][dev.aryan.ecommerceapi.search.ProductSearchQueryBuilder].
 *
 * @property query Free-text search term (multi_match, must-context). `null`/blank means
 *   no text query - a filter-only (or fully open) listing.
 * @property category Exact filter on the category slug (term, filter-context).
 * @property brand Exact filter on brand name (term on `brand.keyword`, filter-context).
 * @property minPrice Inclusive lower price bound; independent of [maxPrice] - either or
 *   both may be set.
 * @property maxPrice Inclusive upper price bound; independent of [minPrice].
 * @property minRating Inclusive lower bound on rating filter.
 * @property inStock `true` filters to `availabilityStatus=IN_STOCK`; `false`/`null` applies no filter.
 * @property sort One of `price_asc`/`price_desc`/`rating_desc`, or `null` for the default
 *   (relevance when [query] is present, alphabetical by title otherwise).
 */
data class ProductSearchParams(
    val query: String?,
    val category: String?,
    val brand: String?,
    val minPrice: Double?,
    val maxPrice: Double?,
    val minRating: Double?,
    val inStock: Boolean?,
    val sort: String?,
    val page: Int,
    val size: Int,
)
