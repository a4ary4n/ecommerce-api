package dev.aryan.ecommerceapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Entry point. A simple e-commerce REST API over MySQL (source of truth) and
 * Elasticsearch (derived search index), with catalog data ingested from dummyjson.
 */
@SpringBootApplication
class EcommerceApiApplication

fun main(args: Array<String>) {
    runApplication<EcommerceApiApplication>(*args)
}
