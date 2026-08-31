package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.OfferDetailsDto
import com.aaspas.customer.data.remote.dto.OfferDetailsShopDto
import com.aaspas.customer.domain.model.OfferVisibilityStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferDetailsMapperTest {

    @Test
    fun `maps active offer details`() {
        val dto = OfferDetailsDto(
            offerId = "offer-1",
            title = "Summer Sale",
            description = "Big discounts",
            photoUrl = "https://example.com/photo.jpg",
            discountType = "percentage",
            discountValue = "20",
            startsAt = "2025-08-19T10:00:00Z",
            endsAt = "2025-08-25T18:00:00Z",
            status = "active",
            shop = OfferDetailsShopDto(
                shopId = "shop-1",
                shopName = "Fashion Hub",
                photoUrl = null,
                category = "Clothing",
                addressArea = "Pimpri",
                latitude = 18.63,
                longitude = 73.80,
                distanceKm = 1.2,
            ),
        )

        val details = OfferDetailsMapper.toDomain(dto)

        assertEquals("offer-1", details.id)
        assertEquals("Summer Sale", details.title)
        assertEquals(OfferVisibilityStatus.ACTIVE, details.status)
        assertEquals("Fashion Hub", details.shop.name)
        assertEquals(1.2, details.shop.distanceKm)
    }

    @Test
    fun `maps coming soon status`() {
        val dto = sampleDto(status = "coming_soon")
        val details = OfferDetailsMapper.toDomain(dto)
        assertEquals(OfferVisibilityStatus.COMING_SOON, details.status)
    }

    @Test
    fun `parse status defaults to active`() {
        assertTrue(OfferDetailsMapper.parseStatus("active") == OfferVisibilityStatus.ACTIVE)
        assertFalse(OfferDetailsMapper.parseStatus("coming_soon") == OfferVisibilityStatus.ACTIVE)
    }

    private fun sampleDto(status: String) = OfferDetailsDto(
        offerId = "offer-2",
        title = "Preview",
        description = null,
        photoUrl = null,
        discountType = "percentage",
        discountValue = "15",
        startsAt = "2025-08-22T10:00:00Z",
        endsAt = "2025-08-30T18:00:00Z",
        status = status,
        shop = OfferDetailsShopDto(
            shopId = "shop-1",
            shopName = "Fashion Hub",
        ),
    )
}
