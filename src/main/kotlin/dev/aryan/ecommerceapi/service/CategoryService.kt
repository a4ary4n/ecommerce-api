package dev.aryan.ecommerceapi.service

import dev.aryan.ecommerceapi.repository.CategoryRepository
import dev.aryan.ecommerceapi.web.dto.CategorySummary
import dev.aryan.ecommerceapi.web.toSummary
import org.springframework.stereotype.Service

/** Backs `GET /categories`. */
@Service
class CategoryService(private val categoryRepository: CategoryRepository) {
    /** All categories, from MySQL. */
    fun listAll(): List<CategorySummary> = categoryRepository.findAll().map { it.toSummary() }
}
