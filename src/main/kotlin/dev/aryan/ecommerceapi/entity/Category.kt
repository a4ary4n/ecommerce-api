package dev.aryan.ecommerceapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * A dummyjson product category (e.g. "smartphones", "fragrances").
 *
 * Seeded from dummyjson's `/products/categories` endpoint during ingestion, not derived
 * from individual products - that endpoint is the only source of the display [name],
 * since a product's own category field is just the [slug].
 *
 * @property id Database-assigned identity, `null` until first persisted.
 * @property slug Stable, URL-safe identifier (e.g. "smartphones") - the value
 *   Elasticsearch filters on and `GET /products?category=` matches against.
 * @property name Human-readable display name (e.g. "Smartphones").
 */
@Entity
@Table(name = "categories")
class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(nullable = false, unique = true, length = 100)
    var slug: String,

    @Column(nullable = false, length = 100)
    var name: String,
)
