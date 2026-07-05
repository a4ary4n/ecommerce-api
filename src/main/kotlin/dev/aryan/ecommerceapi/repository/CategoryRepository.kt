package dev.aryan.ecommerceapi.repository

import dev.aryan.ecommerceapi.entity.Category
import org.springframework.data.jpa.repository.JpaRepository

/** JPA repository for [Category]. */
interface CategoryRepository : JpaRepository<Category, Int> {
    /** Looks up a category by its natural key. Used during ingestion to resolve/dedupe. */
    fun findBySlug(slug: String): Category?
}
