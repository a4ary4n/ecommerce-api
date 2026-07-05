package dev.aryan.ecommerceapi.service

import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import dev.aryan.ecommerceapi.web.dto.ProductSearchParams
import org.springframework.data.domain.PageRequest
import org.springframework.data.elasticsearch.client.elc.NativeQuery

// Pure ProductSearchParams -> NativeQuery construction, no Spring wiring needed - kept
// separate from ProductSearchService, mirroring how ingestion split writer/reader/indexer
// by responsibility.
object ProductSearchQueryBuilder {

    // Elasticsearch's default index.max_result_window - a search request whose
    // from(=page*size)+size exceeds this fails server-side with a search_phase_execution_exception
    // (all shards failed), which otherwise surfaces as an uncaught 500. Validated here instead,
    // same pattern as the page/size >= 0/1 check PageRequest.of() already enforces.
    private const val MAX_RESULT_WINDOW = 10_000L

    fun build(params: ProductSearchParams): NativeQuery {
        val from = params.page.toLong() * params.size
        require(from + params.size <= MAX_RESULT_WINDOW) {
            "page*size + size must not exceed $MAX_RESULT_WINDOW (Elasticsearch's index.max_result_window)"
        }
        // blank ("" or whitespace-only) is treated the same as absent - otherwise an empty
        // multi_match/term query analyzes to zero terms and matches ZERO documents in ES
        // (not "match everything"), which would surprise a client that left a search box
        // blank or omitted a filter with an empty string rather than leaving it off entirely.
        val query = params.query?.trim()?.takeIf { it.isNotEmpty() }
        val category = params.category?.trim()?.takeIf { it.isNotEmpty() }
        val brand = params.brand?.trim()?.takeIf { it.isNotEmpty() }

        val boolQuery = Query.of { q ->
            q.bool { b ->
                query?.let { term ->
                    b.must { m ->
                        m.multiMatch { mm ->
                            mm.query(term)
                                .fields("title^4", "brand^3", "tags^2", "description^1")
                                .fuzziness("AUTO")
                        }
                    }
                }
                category?.let { cat ->
                    b.filter { f -> f.term { t -> t.field("category").value(cat) } }
                }
                // brand filter targets brand.keyword (the exact sub-field) - the plain
                // brand field is analyzed text, used for full-text matching in the multi_match above
                brand?.let { br ->
                    b.filter { f -> f.term { t -> t.field("brand.keyword").value(br) } }
                }
                if (params.minPrice != null || params.maxPrice != null) {
                    b.filter { f ->
                        f.range { r ->
                            r.number { n ->
                                var numberRange = n.field("price")
                                params.minPrice?.let { numberRange = numberRange.gte(it) }
                                params.maxPrice?.let { numberRange = numberRange.lte(it) }
                                numberRange
                            }
                        }
                    }
                }
                // range query on a nullable field (rating) naturally excludes documents
                // missing it - no special null-handling needed
                params.minRating?.let { minR ->
                    b.filter { f -> f.range { r -> r.number { n -> n.field("rating").gte(minR) } } }
                }
                // inStock=false/absent applies no filter at all (checkbox-style "only show
                // in-stock" semantics, not an inverted "only show out-of-stock" filter)
                if (params.inStock == true) {
                    b.filter { f -> f.term { t -> t.field("availabilityStatus").value("IN_STOCK") } }
                }
                b
            }
        }

        val builder = NativeQuery.builder()
            .withQuery(boolQuery)
            .withPageable(PageRequest.of(params.page, params.size))

        // no text query -> a filter-only (or empty) bool query has no natural relevance
        // ordering; tiebreak deterministically. _id/id itself isn't sortable in ES (no
        // doc_values by default on the meta-field), so use title.keyword instead. When
        // a query IS present, leave the default _score/relevance ordering alone. An explicit
        // sort always overrides both. An unrecognized sort value is a 400, not a silent
        // fallback - validating at the boundary rather than swallowing bad input.
        when (params.sort) {
            "price_asc" -> builder.withSort { s -> s.field { f -> f.field("price").order(SortOrder.Asc) } }
            "price_desc" -> builder.withSort { s -> s.field { f -> f.field("price").order(SortOrder.Desc) } }
            "rating_desc" -> builder.withSort { s -> s.field { f -> f.field("rating").order(SortOrder.Desc) } }
            null -> if (query == null) {
                builder.withSort { s -> s.field { f -> f.field("title.keyword").order(SortOrder.Asc) } }
            }
            else -> throw IllegalArgumentException("sort must be one of: price_asc, price_desc, rating_desc")
        }

        return builder.build()
    }
}
