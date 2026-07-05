package dev.aryan.ecommerceapi.search

import co.elastic.clients.elasticsearch._types.SortOrder
import dev.aryan.ecommerceapi.web.dto.ProductSearchParams
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the pure query-construction logic - no Spring context needed. Covers exactly
 * the validation/filter-composition behavior that was previously only verified by hand against
 * the live containers (see CLAUDE.md's ingestion/search sections for the manual verification
 * this now locks in as a regression test).
 */
class ProductSearchQueryBuilderTest {

    private fun params(
        query: String? = null,
        category: String? = null,
        brand: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        minRating: Double? = null,
        inStock: Boolean? = null,
        sort: String? = null,
        page: Int = 0,
        size: Int = 20,
    ) = ProductSearchParams(query, category, brand, minPrice, maxPrice, minRating, inStock, sort, page, size)

    @Test
    fun `throws when page times size plus size exceeds the ES result window`() {
        val ex = assertThrows(InvalidSearchParameterException::class.java) {
            ProductSearchQueryBuilder.build(params(page = 150, size = 100))
        }
        assertTrue(ex.message!!.contains("index.max_result_window"))
    }

    @Test
    fun `does not throw exactly at the result window boundary`() {
        // page*size + size == 10_000 exactly: 99*100 + 100 = 10_000
        assertDoesNotThrow { ProductSearchQueryBuilder.build(params(page = 99, size = 100)) }
    }

    @Test
    fun `throws for an unrecognized sort value`() {
        val ex = assertThrows(InvalidSearchParameterException::class.java) {
            ProductSearchQueryBuilder.build(params(sort = "bogus"))
        }
        assertTrue(ex.message!!.contains("price_asc"))
    }

    @Test
    fun `does not throw for each valid sort value`() {
        listOf("price_asc", "price_desc", "rating_desc").forEach { sort ->
            assertDoesNotThrow { ProductSearchQueryBuilder.build(params(sort = sort)) }
        }
    }

    @Test
    fun `blank query is treated as absent - no must clause`() {
        val query = ProductSearchQueryBuilder.build(params(query = "   ")).query!!
        assertTrue(query.bool().must().isEmpty())
    }

    @Test
    fun `non-blank query adds a weighted multi_match must clause with fuzziness`() {
        val query = ProductSearchQueryBuilder.build(params(query = "phone")).query!!
        val must = query.bool().must()
        assertEquals(1, must.size)
        val multiMatch = must[0].multiMatch()
        assertEquals("phone", multiMatch.query())
        assertEquals(listOf("title^4", "brand^3", "tags^2", "description^1"), multiMatch.fields())
        assertEquals("AUTO", multiMatch.fuzziness())
    }

    @Test
    fun `blank category and brand are treated as absent - no filters`() {
        val query = ProductSearchQueryBuilder.build(params(category = " ", brand = "")).query!!
        assertTrue(query.bool().filter().isEmpty())
    }

    @Test
    fun `category filter is an exact term match on the category field`() {
        val query = ProductSearchQueryBuilder.build(params(category = "smartphones")).query!!
        val term = query.bool().filter().single().term()
        assertEquals("category", term.field())
        assertEquals("smartphones", term.value().stringValue())
    }

    @Test
    fun `brand filter targets the keyword sub-field, not the analyzed text field`() {
        val query = ProductSearchQueryBuilder.build(params(brand = "Apple")).query!!
        val term = query.bool().filter().single().term()
        assertEquals("brand.keyword", term.field())
        assertEquals("Apple", term.value().stringValue())
    }

    @Test
    fun `minPrice and maxPrice set independent bounds on one range filter`() {
        val range = ProductSearchQueryBuilder.build(params(minPrice = 10.0, maxPrice = 50.0))
            .query!!.bool().filter().single().range().number()
        assertEquals("price", range.field())
        assertEquals(10.0, range.gte())
        assertEquals(50.0, range.lte())
    }

    @Test
    fun `only minPrice given leaves the upper bound unset`() {
        val range = ProductSearchQueryBuilder.build(params(minPrice = 10.0))
            .query!!.bool().filter().single().range().number()
        assertEquals(10.0, range.gte())
        assertNull(range.lte())
    }

    @Test
    fun `minRating filter is gte-only on the rating field`() {
        val range = ProductSearchQueryBuilder.build(params(minRating = 4.5))
            .query!!.bool().filter().single().range().number()
        assertEquals("rating", range.field())
        assertEquals(4.5, range.gte())
        assertNull(range.lte())
    }

    @Test
    fun `inStock=true adds an availabilityStatus term filter`() {
        val term = ProductSearchQueryBuilder.build(params(inStock = true)).query!!.bool().filter().single().term()
        assertEquals("availabilityStatus", term.field())
        assertEquals("IN_STOCK", term.value().stringValue())
    }

    @Test
    fun `inStock=false applies no filter - checkbox semantics, not inverted`() {
        val query = ProductSearchQueryBuilder.build(params(inStock = false)).query!!
        assertTrue(query.bool().filter().isEmpty())
    }

    @Test
    fun `inStock absent applies no filter`() {
        val query = ProductSearchQueryBuilder.build(params(inStock = null)).query!!
        assertTrue(query.bool().filter().isEmpty())
    }

    @Test
    fun `explicit sort is applied even when a text query is present`() {
        val nativeQuery = ProductSearchQueryBuilder.build(params(query = "phone", sort = "price_asc"))
        val fieldSort = nativeQuery.sortOptions.single().field()
        assertEquals("price", fieldSort.field())
        assertEquals(SortOrder.Asc, fieldSort.order())
    }

    @Test
    fun `rating_desc sorts descending on rating`() {
        val fieldSort = ProductSearchQueryBuilder.build(params(sort = "rating_desc")).sortOptions.single().field()
        assertEquals("rating", fieldSort.field())
        assertEquals(SortOrder.Desc, fieldSort.order())
    }

    @Test
    fun `no query and no explicit sort falls back to alphabetical by title`() {
        val fieldSort = ProductSearchQueryBuilder.build(params()).sortOptions.single().field()
        assertEquals("title.keyword", fieldSort.field())
        assertEquals(SortOrder.Asc, fieldSort.order())
    }

    @Test
    fun `query present with no explicit sort leaves relevance ordering alone`() {
        val nativeQuery = ProductSearchQueryBuilder.build(params(query = "phone"))
        assertTrue(nativeQuery.sortOptions.isEmpty())
    }

    @Test
    fun `page and size are passed through to the pageable`() {
        val nativeQuery = ProductSearchQueryBuilder.build(params(page = 2, size = 10))
        assertEquals(2, nativeQuery.pageable.pageNumber)
        assertEquals(10, nativeQuery.pageable.pageSize)
    }

    @Test
    fun `every param composes together in a single bool query`() {
        val query = ProductSearchQueryBuilder.build(
            params(
                query = "phone",
                category = "smartphones",
                brand = "Apple",
                minPrice = 1.0,
                maxPrice = 999.0,
                minRating = 4.0,
                inStock = true,
            )
        ).query!!
        assertEquals(1, query.bool().must().size)
        assertEquals(5, query.bool().filter().size) // category, brand, price range, rating, inStock
    }
}
