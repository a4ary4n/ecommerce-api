package dev.aryan.ecommerceapi.repository

import dev.aryan.ecommerceapi.entity.Brand
import org.springframework.data.jpa.repository.JpaRepository

interface BrandRepository : JpaRepository<Brand, Int> {
    fun findByName(name: String): Brand?
}
