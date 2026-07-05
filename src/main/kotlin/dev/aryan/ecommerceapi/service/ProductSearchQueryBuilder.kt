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

    fun build(params: ProductSearchParams): NativeQuery {
        // blank ("" or whitespace-only) is treated the same as absent - otherwise an empty
        // multi_match/term query analyzes to zero terms and matches ZERO documents in ES
        // (not "match everything"), which would surprise a client that left a search box
        // blank or omitted a filter with an empty string rather than leaving it off entirely.
        val query = params.query?.trim()?.takeIf { it.isNotEmpty() }
        val category = params.category?.trim()?.takeIf { it.isNotEmpty() }

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
                b
            }
        }

        val builder = NativeQuery.builder()
            .withQuery(boolQuery)
            .withPageable(PageRequest.of(params.page, params.size))

        // no text query -> a filter-only (or empty) bool query has no natural relevance
        // ordering; tiebreak deterministically. _id/id itself isn't sortable in ES (no
        // doc_values by default on the meta-field), so use title.keyword instead. When
        // a query IS present, leave the default _score/relevance ordering alone.
        if (query == null) {
            builder.withSort { s -> s.field { f -> f.field("title.keyword").order(SortOrder.Asc) } }
        }

        return builder.build()
    }
}
