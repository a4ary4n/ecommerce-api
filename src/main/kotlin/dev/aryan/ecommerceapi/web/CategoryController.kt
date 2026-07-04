package dev.aryan.ecommerceapi.web

import dev.aryan.ecommerceapi.service.CategoryService
import dev.aryan.ecommerceapi.web.dto.CategorySummary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/categories")
class CategoryController(private val categoryService: CategoryService) {

    @GetMapping
    fun list(): List<CategorySummary> = categoryService.listAll()
}
