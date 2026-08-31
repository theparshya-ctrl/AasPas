package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.ShopDetailsDto
import com.aaspas.customer.data.remote.dto.ShopOfferItemDto
import com.aaspas.customer.domain.model.OfferVisibilityStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopDetailsMapperTest {

    @Test
    fun `maps shop details with offers`() {
        val dto = ShopDetailsDto(
            shopId = "shop-1",
            shopName = "Fashion Hub",
            description = "Local fashion store",
            category = "Clothing",
            addressLine1 = "Market Road",
            city = "Pimpri",
            latitude = 18.63,
            longitude = 73.80,
            phone = "+919876543210",
            businessHours = mapOf("opens_at" to "09:00", "closes_at" to "21:00"),
            isVerified = true,
            activeOfferCount = 1,
            distanceKm = 2.0,
            todayOffers = listOf(
                ShopOfferItemDto(
                    offerId = "offer-1",
                    title = "Today Deal",
                    discountType = "percentage",
                    discountValue = "10",
                    startsAt = "2025-08-19T10:00:00Z",
                    endsAt = "2025-08-25T18:00:00Z",
                    status = "active",
                ),
            ),
            comingSoon = listOf(
                ShopOfferItemDto(
                    offerId = "offer-2",
                    title = "Festive Preview",
                    discountType = "percentage",
                    discountValue = "15",
                    startsAt = "2025-08-22T10:00:00Z",
                    endsAt = "2025-08-30T18:00:00Z",
                    status = "coming_soon",
                ),
            ),
        )

        val details = ShopDetailsMapper.toDomain(dto)

        assertEquals("Fashion Hub", details.name)
        assertTrue(details.isVerified)
        assertEquals(1, details.todayOffers.size)
        assertEquals(OfferVisibilityStatus.ACTIVE, details.todayOffers.first().status)
        assertEquals(1, details.comingSoon.size)
        assertEquals(OfferVisibilityStatus.COMING_SOON, details.comingSoon.first().status)
        assertEquals("09:00", details.businessHours?.opensAt)
    }
}
