package dev.aryan.ecommerceapi.web

import dev.aryan.ecommerceapi.service.CategoryService
import dev.aryan.ecommerceapi.web.dto.CategorySummary
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** `@WebMvcTest` slice test for [CategoryController], [CategoryService] mocked out. */
@WebMvcTest(CategoryController::class)
class CategoryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var categoryService: CategoryService

    @Test
    fun `list returns 200 with every category the service provides`() {
        given(categoryService.listAll()).willReturn(
            listOf(
                CategorySummary(1, "smartphones", "Smartphones"),
                CategorySummary(2, "fragrances", "Fragrances"),
            )
        )

        mockMvc.perform(get("/categories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].slug").value("smartphones"))
            .andExpect(jsonPath("$[1].name").value("Fragrances"))
    }
}
