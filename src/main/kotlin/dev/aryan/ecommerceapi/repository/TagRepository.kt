package dev.aryan.ecommerceapi.repository

import dev.aryan.ecommerceapi.entity.Tag
import org.springframework.data.jpa.repository.JpaRepository

/** JPA repository for [Tag]. */
interface TagRepository : JpaRepository<Tag, Int> {
    /** Looks up a tag by its natural key. Used during ingestion to resolve/dedupe. */
    fun findByName(name: String): Tag?
}
