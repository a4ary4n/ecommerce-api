package dev.aryan.ecommerceapi.service

import dev.aryan.ecommerceapi.repository.CategoryRepository
import dev.aryan.ecommerceapi.web.dto.CategorySummary
import dev.aryan.ecommerceapi.web.toSummary
import org.springframework.stereotype.Service

@Service
class CategoryService(private val categoryRepository: CategoryRepository) {
    fun listAll(): List<CategorySummary> = categoryRepository.findAll().map { it.toSummary() }
}
