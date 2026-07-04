package dev.aryan.ecommerceapi.repository

import dev.aryan.ecommerceapi.entity.Tag
import org.springframework.data.jpa.repository.JpaRepository

interface TagRepository : JpaRepository<Tag, Int> {
    fun findByName(name: String): Tag?
}
