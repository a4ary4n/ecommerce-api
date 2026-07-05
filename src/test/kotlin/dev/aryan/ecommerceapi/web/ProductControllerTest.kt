package dev.aryan.ecommerceapi.web

import dev.aryan.ecommerceapi.search.InvalidSearchParameterException
import dev.aryan.ecommerceapi.service.ProductDetailService
import dev.aryan.ecommerceapi.service.ProductSearchService
import dev.aryan.ecommerceapi.web.dto.CategorySummary
import dev.aryan.ecommerceapi.web.dto.PageResponse
import dev.aryan.ecommerceapi.web.dto.ProductDetailResponse
import dev.aryan.ecommerceapi.web.dto.ProductSearchParams
import dev.aryan.ecommerceapi.web.dto.ProductSummaryResponse
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * `@WebMvcTest` slice tests for [ProductController] - real Spring MVC dispatch (validation,
 * exception handling, path-variable conversion) with [ProductSearchService]/
 * [ProductDetailService] mocked out, so no MySQL/Elasticsearch needed. Locks in exactly the
 * validation/exception behavior CLAUDE.md documents as having gone through real bugs
 * (page/size bounds, the `@Validated` trap, and the `IllegalArgumentException` handler being
 * too broad for `/products/{id}`).
 */
@WebMvcTest(ProductController::class)
class ProductControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var productSearchService: ProductSearchService

    @MockitoBean
    private lateinit var productDetailService: ProductDetailService

    private fun sampleSummary() = ProductSummaryResponse(
        id = 1,
        title = "iPhone",
        description = "A phone",
        category = "smartphones",
        brand = "Apple",
        tags = listOf("featured"),
        price = 999.99,
        discountPercentage = 10.0,
        rating = 4.5,
        stock = 10,
        availabilityStatus = "IN_STOCK",
        thumbnail = "https://example.com/thumb.png",
        createdAt = LocalDateTime.of(2024, 1, 1, 0, 0),
    )

    private fun sampleDetail() = ProductDetailResponse(
        id = 1,
        title = "iPhone",
        description = "A phone",
        category = CategorySummary(1, "smartphones", "Smartphones"),
        brand = "Apple",
        sku = "SKU1",
        price = BigDecimal("999.99"),
        discountPercentage = BigDecimal("10.0"),
        rating = BigDecimal("4.5"),
        stock = 10,
        availabilityStatus = "IN_STOCK",
        weight = null,
        width = null,
        height = null,
        depth = null,
        warrantyInformation = null,
        shippingInformation = null,
        returnPolicy = null,
        minimumOrderQuantity = null,
        thumbnail = null,
        barcode = null,
        qrCode = null,
        createdAt = null,
        updatedAt = null,
        tags = emptyList(),
        images = emptyList(),
        reviews = emptyList(),
    )

    @Test
    fun `search returns 200 with the service's paginated result`() {
        val params = ProductSearchParams(
            query = "phone", category = null, brand = null, minPrice = null, maxPrice = null,
            minRating = null, inStock = null, sort = null, page = 0, size = 20,
        )
        given(productSearchService.search(params)).willReturn(
            PageResponse(content = listOf(sampleSummary()), page = 0, size = 20, totalElements = 1, totalPages = 1)
        )

        mockMvc.perform(get("/products?query=phone"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("iPhone"))
            .andExpect(jsonPath("$.content[0].brand").value("Apple"))
    }

    @Test
    fun `search defaults page and size when omitted`() {
        val params = ProductSearchParams(null, null, null, null, null, null, null, null, 0, 20)
        given(productSearchService.search(params)).willReturn(
            PageResponse(content = emptyList(), page = 0, size = 20, totalElements = 0, totalPages = 0)
        )
        mockMvc.perform(get("/products")).andExpect(status().isOk)
    }

    @Test
    fun `size=0 is rejected with 400`() {
        mockMvc.perform(get("/products?size=0")).andExpect(status().isBadRequest)
    }

    @Test
    fun `size=101 is rejected with 400`() {
        mockMvc.perform(get("/products?size=101")).andExpect(status().isBadRequest)
    }

    @Test
    fun `size=100 at the upper boundary is accepted`() {
        val params = ProductSearchParams(null, null, null, null, null, null, null, null, 0, 100)
        given(productSearchService.search(params)).willReturn(
            PageResponse(content = emptyList(), page = 0, size = 100, totalElements = 0, totalPages = 0)
        )
        mockMvc.perform(get("/products?size=100")).andExpect(status().isOk)
    }

    @Test
    fun `page=-1 is rejected with 400`() {
        mockMvc.perform(get("/products?page=-1")).andExpect(status().isBadRequest)
    }

    @Test
    fun `minPrice=-5 is rejected with 400`() {
        mockMvc.perform(get("/products?minPrice=-5")).andExpect(status().isBadRequest)
    }

    @Test
    fun `minRating=6 is rejected with 400`() {
        mockMvc.perform(get("/products?minRating=6")).andExpect(status().isBadRequest)
    }

    @Test
    fun `an InvalidSearchParameterException from the service is mapped to 400 with an error body`() {
        val params = ProductSearchParams(null, null, null, null, null, null, null, "bogus", 0, 20)
        given(productSearchService.search(params))
            .willThrow(InvalidSearchParameterException("sort must be one of: price_asc, price_desc, rating_desc"))

        mockMvc.perform(get("/products?sort=bogus"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("sort must be one of: price_asc, price_desc, rating_desc"))
    }

    @Test
    fun `getById returns 200 with full detail when found`() {
        given(productDetailService.getById(1)).willReturn(sampleDetail())
        mockMvc.perform(get("/products/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("iPhone"))
            .andExpect(jsonPath("$.category.slug").value("smartphones"))
    }

    @Test
    fun `getById returns 404 when the product does not exist`() {
        given(productDetailService.getById(999999)).willReturn(null)
        mockMvc.perform(get("/products/999999")).andExpect(status().isNotFound)
    }

    @Test
    fun `getById with a non-numeric id returns Spring's own 400, not our custom error body`() {
        // Regression test: NumberFormatException (from parsing "abc") is an IllegalArgumentException,
        // and was previously caught by a too-broad handler meant only for search business rules.
        mockMvc.perform(get("/products/abc"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").doesNotExist())
    }
}
