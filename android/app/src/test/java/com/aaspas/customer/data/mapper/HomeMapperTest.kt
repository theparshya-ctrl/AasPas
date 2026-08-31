package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.HomeCategoryDto
import com.aaspas.customer.data.remote.dto.HomeDataDto
import com.aaspas.customer.data.remote.dto.HomeLocationDto
import com.aaspas.customer.data.remote.dto.HomeOfferDto
import com.aaspas.customer.data.remote.dto.HomeShopDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeMapperTest {

    @Test
    fun `maps home dto to domain feed`() {
        val dto = HomeDataDto(
            location = HomeLocationDto(latitude = 18.63, longitude = 73.80, available = true),
            categories = listOf(
                HomeCategoryDto(id = "cat-1", name = "Clothing", slug = "clothing", displayOrder = 1),
            ),
            todayOffers = listOf(sampleOffer("offer-1")),
            comingSoon = listOf(sampleOffer("offer-2")),
            nearbyShops = listOf(
                HomeShopDto(
                    shopId = "shop-1",
                    shopName = "Fashion Hub",
                    photoUrl = null,
                    category = "Clothing",
                    addressArea = "Pimpri",
                    distanceKm = 1.2,
                    latitude = 18.63,
                    longitude = 73.80,
                ),
            ),
        )

        val feed = HomeMapper.toDomain(dto)

        assertTrue(feed.location.available)
        assertEquals(1, feed.categories.size)
        assertEquals("Clothing", feed.categories.first().name)
        assertEquals(1, feed.todayOffers.size)
        assertFalse(feed.todayOffers.first().isComingSoon)
        assertEquals(1, feed.comingSoon.size)
        assertTrue(feed.comingSoon.first().isComingSoon)
        assertEquals("Fashion Hub", feed.nearbyShops.first().name)
    }

    @Test
    fun `maps empty sections`() {
        val dto = HomeDataDto(
            location = HomeLocationDto(available = false),
        )

        val feed = HomeMapper.toDomain(dto)

        assertFalse(feed.location.available)
        assertTrue(feed.categories.isEmpty())
        assertTrue(feed.todayOffers.isEmpty())
        assertTrue(feed.comingSoon.isEmpty())
        assertTrue(feed.nearbyShops.isEmpty())
    }

    private fun sampleOffer(id: String) = HomeOfferDto(
        offerId = id,
        title = "Summer Sale",
        description = "Big discounts",
        photoUrl = "https://example.com/photo.jpg",
        discountType = "percentage",
        discountValue = "20",
        startsAt = "2025-08-22T10:00:00Z",
        endsAt = "2025-08-30T18:00:00Z",
        shopId = "shop-1",
        shopName = "Fashion Hub",
        distanceKm = 2.5,
        category = "Clothing",
    )
}
