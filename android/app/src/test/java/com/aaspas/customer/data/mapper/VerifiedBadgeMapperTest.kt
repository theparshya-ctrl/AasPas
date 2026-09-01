package com.aaspas.customer.data.mapper

import com.aaspas.customer.domain.model.OfferVisibilityStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import com.aaspas.customer.core.common.DateFormatters
import org.junit.Test

class VerifiedBadgeMapperTest {

    @Test
    fun `offer details maps verified flag`() {
        val dto = com.aaspas.customer.data.remote.dto.OfferDetailsDto(
            offerId = "offer-1",
            title = "Sale",
            discountType = "percentage",
            discountValue = "10",
            startsAt = "2026-08-21T09:00:00Z",
            endsAt = "2026-08-21T21:00:00Z",
            status = "active",
            isVerified = true,
            shop = com.aaspas.customer.data.remote.dto.OfferDetailsShopDto(
                shopId = "shop-1",
                shopName = "Fresh Mart",
            ),
        )

        assertTrue(OfferDetailsMapper.toDomain(dto).isVerified)
    }

    @Test
    fun `home offer defaults verified to false`() {
        val dto = com.aaspas.customer.data.remote.dto.HomeOfferDto(
            offerId = "offer-1",
            title = "Sale",
            discountType = "percentage",
            discountValue = "10",
            startsAt = "2026-08-21T09:00:00Z",
            endsAt = "2026-08-21T21:00:00Z",
            shopId = "shop-1",
            shopName = "Fresh Mart",
        )

        val offer = HomeMapper.toDomain(
            com.aaspas.customer.data.remote.dto.HomeDataDto(
                location = com.aaspas.customer.data.remote.dto.HomeLocationDto(),
                todayOffers = listOf(dto),
            ),
        ).todayOffers.first()

        assertFalse(offer.isVerified)
    }

    @Test
    fun `home offer maps external source fields`() {
        val dto = com.aaspas.customer.data.remote.dto.HomeOfferDto(
            offerId = "offer-ext-1",
            title = "External Sale",
            discountType = "percentage",
            discountValue = "10",
            startsAt = "2026-08-21T09:00:00Z",
            endsAt = "2026-08-21T21:00:00Z",
            shopId = "shop-1",
            shopName = "Public Shop",
            isVerified = false,
            sourceType = "EXTERNAL",
            sourceName = "Public website",
        )

        val offer = HomeMapper.toDomain(
            com.aaspas.customer.data.remote.dto.HomeDataDto(
                location = com.aaspas.customer.data.remote.dto.HomeLocationDto(),
                todayOffers = listOf(dto),
            ),
        ).todayOffers.first()

        assertFalse(offer.isVerified)
        assertTrue(offer.isExternal)
        assertEquals("Public website", offer.sourceName)
    }

    @Test
    fun `favorite offer maps is verified from dto`() {
        val dto = com.aaspas.customer.data.remote.dto.FavoriteOfferDto(
            favoriteId = "fav-1",
            offerId = "offer-1",
            title = "Saved External",
            discountType = "percentage",
            discountValue = "10",
            shopId = "shop-1",
            shopName = "Public Shop",
            isVerified = false,
            sourceType = "EXTERNAL",
            sourceName = "Instagram",
            savedAt = "2026-08-21T09:00:00Z",
        )

        val offer = FavoritesMapper.toDomain(
            com.aaspas.customer.data.remote.dto.FavoritesDataDto(offers = listOf(dto)),
        ).offers.first().offer

        assertFalse(offer.isVerified)
        assertTrue(offer.isExternal)
        assertEquals("Instagram", offer.sourceName)
    }

    @Test
    fun `format offer value preserves up to wording`() {
        val formatted = DateFormatters.formatOfferValue("up_to_percentage", "70")
        assertEquals("Up to 70% off", formatted)
    }
}
