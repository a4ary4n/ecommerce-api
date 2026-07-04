package dev.aryan.ecommerceapi.ingestion

import dev.aryan.ecommerceapi.repository.ProductRepository
import dev.aryan.ecommerceapi.repository.ProductTagRepository
import dev.aryan.ecommerceapi.search.ProductDocument
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductCatalogReader(
    private val productRepository: ProductRepository,
    private val productTagRepository: ProductTagRepository,
) {

    @Transactional(readOnly = true)
    fun readAllAsDocuments(): List<ProductDocument> {
        val products = productRepository.findAllWithCategoryAndBrand()

        val tagsByProductId = productTagRepository
            .findTagNamesForProducts(products.map { it.id })
            .groupBy({ it.getProductId() }, { it.getTagName() })

        return products.map { product ->
            product.toDocument(tagsByProductId[product.id].orEmpty())
        }
    }
}
