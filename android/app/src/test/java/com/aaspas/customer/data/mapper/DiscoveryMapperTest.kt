package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.SearchOfferDto
import com.aaspas.customer.data.remote.dto.SearchResultsDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryMapperTest {

    @Test
    fun `maps search results to domain offers`() {
        val dto = SearchResultsDto(
            offers = listOf(
                SearchOfferDto(
                    offerId = "offer-1",
                    title = "Shirt Sale",
                    description = "Summer",
                    photoUrl = null,
                    discountType = "percentage",
                    discountValue = "20",
                    startsAt = "2025-08-19T10:00:00Z",
                    endsAt = "2025-08-25T18:00:00Z",
                    status = "active",
                    shopId = "shop-1",
                    shopName = "Fashion Hub",
                    category = "Clothing",
                    distanceKm = 1.5,
                ),
                SearchOfferDto(
                    offerId = "offer-2",
                    title = "Preview",
                    discountType = "percentage",
                    discountValue = "10",
                    startsAt = "2025-08-22T10:00:00Z",
                    endsAt = "2025-08-30T18:00:00Z",
                    status = "coming_soon",
                    shopId = "shop-1",
                    shopName = "Fashion Hub",
                ),
            ),
            total = 2,
            page = 1,
            pageSize = 20,
            totalPages = 1,
        )

        val results = DiscoveryMapper.toSearchResults(dto)

        assertEquals(2, results.offers.size)
        assertFalse(results.offers.first().isComingSoon)
        assertTrue(results.offers.last().isComingSoon)
        assertEquals("Fashion Hub", results.offers.first().shopName)
    }
}
