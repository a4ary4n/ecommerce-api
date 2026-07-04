package dev.aryan.ecommerceapi.repository

import dev.aryan.ecommerceapi.entity.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Int> {
    fun findBySlug(slug: String): Category?
}
