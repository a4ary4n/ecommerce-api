package dev.aryan.ecommerceapi.service

import dev.aryan.ecommerceapi.repository.CategoryRepository
import dev.aryan.ecommerceapi.web.dto.CategorySummary
import dev.aryan.ecommerceapi.web.toSummary
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/** Backs `GET /categories`. */
@Service
class CategoryService(private val categoryRepository: CategoryRepository) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** All categories, from MySQL. */
    fun listAll(): List<CategorySummary> {
        val categories = categoryRepository.findAll().map { it.toSummary() }
        log.debug("listed {} categories", categories.size)
        return categories
    }
}
