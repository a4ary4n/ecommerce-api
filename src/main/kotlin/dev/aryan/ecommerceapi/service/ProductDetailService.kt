package dev.aryan.ecommerceapi.service

import dev.aryan.ecommerceapi.repository.ProductImageRepository
import dev.aryan.ecommerceapi.repository.ProductRepository
import dev.aryan.ecommerceapi.repository.ProductTagRepository
import dev.aryan.ecommerceapi.repository.ReviewRepository
import dev.aryan.ecommerceapi.web.dto.ProductDetailResponse
import dev.aryan.ecommerceapi.web.toDetailResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductDetailService(
    private val productRepository: ProductRepository,
    private val reviewRepository: ReviewRepository,
    private val productImageRepository: ProductImageRepository,
    private val productTagRepository: ProductTagRepository,
) {
    // readOnly transaction keeps the session open so product.category/product.brand
    // (both LAZY) can resolve here despite open-in-view: false
    @Transactional(readOnly = true)
    fun getById(id: Int): ProductDetailResponse? {
        val product = productRepository.findById(id).orElse(null) ?: return null
        val reviews = reviewRepository.findByProductId(id)
        val images = productImageRepository.findByProductIdOrderBySortOrderAsc(id)
        val tags = productTagRepository.findTagNamesForProducts(listOf(id)).map { it.getTagName() }
        return product.toDetailResponse(reviews, images, tags)
    }
}
