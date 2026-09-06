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

    @Test
    fun `maps external shop offer with null ends_at`() {
        val dto = ShopDetailsDto(
            shopId = "shop-ext-1",
            shopName = "Kirandeep Mobile Stores",
            isVerified = false,
            todayOffers = listOf(
                ShopOfferItemDto(
                    offerId = "offer-ext-1",
                    title = "Save 5% on store vouchers",
                    discountType = "percentage",
                    discountValue = "5",
                    startsAt = "2026-09-01T00:00:00Z",
                    endsAt = null,
                    status = "active",
                    isVerified = false,
                    sourceType = "EXTERNAL",
                    sourceName = "Magicpin",
                ),
            ),
        )

        val details = ShopDetailsMapper.toDomain(dto)

        assertEquals(1, details.todayOffers.size)
        assertEquals(null, details.todayOffers.first().endsAt)
        assertEquals(OfferVisibilityStatus.ACTIVE, details.todayOffers.first().status)
        assertEquals(false, details.todayOffers.first().isVerified)
        assertEquals("EXTERNAL", details.todayOffers.first().sourceType)
        assertEquals("Magicpin", details.todayOffers.first().sourceName)
        assertTrue(details.todayOffers.first().isExternal)
    }

    @Test
    fun `maps verified internal shop offer without external disclaimer fields`() {
        val dto = ShopDetailsDto(
            shopId = "shop-1",
            shopName = "Verified Shop",
            todayOffers = listOf(
                ShopOfferItemDto(
                    offerId = "offer-1",
                    title = "Today Deal",
                    discountType = "percentage",
                    discountValue = "10",
                    status = "active",
                    isVerified = true,
                    sourceType = "AASPAS",
                ),
            ),
        )

        val offer = ShopDetailsMapper.toDomain(dto).todayOffers.first()

        assertTrue(offer.isVerified)
        assertEquals("AASPAS", offer.sourceType)
        assertEquals(false, offer.isExternal)
    }
}
