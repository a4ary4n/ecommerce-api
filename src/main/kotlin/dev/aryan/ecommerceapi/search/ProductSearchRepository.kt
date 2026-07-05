package dev.aryan.ecommerceapi.search

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

/**
 * Spring Data Elasticsearch repository for [ProductDocument]. Used for the ingestion
 * bulk-indexing path ([ElasticsearchIndexer][dev.aryan.ecommerceapi.ingestion.ElasticsearchIndexer]) -
 * ad-hoc composable search queries (`GET /products`) bypass this and use
 * [org.springframework.data.elasticsearch.core.ElasticsearchOperations] directly instead,
 * since repository method derivation can't express a dynamic multi-filter bool query.
 */
interface ProductSearchRepository : ElasticsearchRepository<ProductDocument, String>
