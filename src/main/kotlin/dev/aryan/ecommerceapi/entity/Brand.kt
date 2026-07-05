package dev.aryan.ecommerceapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * A product brand (e.g. "Apple", "Samsung").
 *
 * Resolved-or-created during ingestion as products are processed - dummyjson has no
 * separate brands endpoint, so [name] is the only identifying data available.
 *
 * @property id Database-assigned identity, `null` until first persisted.
 * @property name Brand name; unique, doubles as the natural key ingestion resolves by.
 */
@Entity
@Table(name = "brands")
class Brand(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(nullable = false, unique = true, length = 150)
    var name: String,
)
