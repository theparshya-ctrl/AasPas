package com.aaspas.customer.data.mapper

import com.aaspas.customer.domain.model.OfferVisibilityStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
}
