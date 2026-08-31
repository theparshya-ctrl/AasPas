package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.FavoriteOfferDto
import com.aaspas.customer.data.remote.dto.FavoriteShopDto
import com.aaspas.customer.data.remote.dto.FavoritesDataDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesMapperTest {

    @Test
    fun `maps favorites dto to domain feed`() {
        val dto = FavoritesDataDto(
            offers = listOf(
                FavoriteOfferDto(
                    favoriteId = "fav-1",
                    offerId = "offer-1",
                    title = "Summer Sale",
                    discountType = "percentage",
                    discountValue = "20",
                    shopId = "shop-1",
                    shopName = "Fashion Hub",
                    status = "active",
                    isActive = true,
                    savedAt = "2025-08-19T10:00:00Z",
                ),
            ),
            shops = listOf(
                FavoriteShopDto(
                    favoriteId = "fav-2",
                    shopId = "shop-1",
                    shopName = "Fashion Hub",
                    category = "Clothing",
                    addressArea = "Pimpri",
                    distanceKm = 1.2,
                    latitude = 18.63,
                    longitude = 73.80,
                    isActive = true,
                    savedAt = "2025-08-19T10:00:00Z",
                ),
            ),
        )

        val feed = FavoritesMapper.toDomain(dto)

        assertEquals(1, feed.offers.size)
        assertEquals("offer-1", feed.offers.first().offer.id)
        assertTrue(feed.offers.first().offer.isSaved)
        assertTrue(feed.offers.first().isActive)
        assertEquals(1, feed.shops.size)
        assertEquals("Fashion Hub", feed.shops.first().shop.name)
        assertTrue(feed.shops.first().shop.isSaved)
    }

    @Test
    fun `maps inactive favorite offer`() {
        val dto = FavoritesDataDto(
            offers = listOf(
                FavoriteOfferDto(
                    favoriteId = "fav-1",
                    offerId = "offer-1",
                    title = "Expired Deal",
                    discountType = "percentage",
                    discountValue = "10",
                    shopId = "shop-1",
                    shopName = "Fashion Hub",
                    status = "expired",
                    isActive = false,
                    savedAt = "2025-08-01T10:00:00Z",
                ),
            ),
        )

        val favorite = FavoritesMapper.toDomain(dto).offers.first()

        assertFalse(favorite.isActive)
        assertFalse(favorite.offer.isComingSoon)
    }
}
