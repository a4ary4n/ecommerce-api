package dev.aryan.ecommerceapi.repository

import dev.aryan.ecommerceapi.entity.Brand
import org.springframework.data.jpa.repository.JpaRepository

/** JPA repository for [Brand]. */
interface BrandRepository : JpaRepository<Brand, Int> {
    /** Looks up a brand by its natural key. Used during ingestion to resolve/dedupe. */
    fun findByName(name: String): Brand?
}
