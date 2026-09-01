package com.aaspas.customer.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression: live Beta external offers use ends_at=null (CURSOR-050/051).
 */
class ExternalOfferDtoParseTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `home offer parses null ends_at`() {
        val offer = decodeHomeOffer()
        assertNull(offer.endsAt)
        assertEquals("EXTERNAL", offer.sourceType)
        assertEquals(false, offer.isVerified)
    }

    @Test
    fun `search offer parses null ends_at`() {
        val offer = json.decodeFromString<SearchOfferDto>(
            """
            {
              "offer_id": "fe7d6abd-1d78-45c9-8312-2efc67e0584b",
              "title": "Save 5% on store vouchers",
              "discount_type": "percentage",
              "discount_value": "5.00",
              "starts_at": "2026-09-01T00:00:00Z",
              "ends_at": null,
              "status": "active",
              "shop_id": "f85595a3-43f4-4f19-bc7e-f5c2f187d91f",
              "shop_name": "Kirandeep Mobile Stores",
              "is_verified": false,
              "source_type": "EXTERNAL",
              "source_name": "Magicpin"
            }
            """.trimIndent(),
        )
        assertNull(offer.endsAt)
        assertEquals("EXTERNAL", offer.sourceType)
    }

    @Test
    fun `offer details parses null ends_at with external fields`() {
        val response = json.decodeFromString<ApiResponseDto<OfferDetailsDto>>(
            """
            {
              "success": true,
              "data": {
                "offer_id": "fe7d6abd-1d78-45c9-8312-2efc67e0584b",
                "title": "Save 5% on store vouchers",
                "discount_type": "percentage",
                "discount_value": "5.00",
                "starts_at": "2026-09-01T00:00:00Z",
                "ends_at": null,
                "status": "active",
                "is_verified": false,
                "source_type": "EXTERNAL",
                "source_name": "Magicpin",
                "shop": {
                  "shop_id": "f85595a3-43f4-4f19-bc7e-f5c2f187d91f",
                  "shop_name": "Kirandeep Mobile Stores"
                }
              }
            }
            """.trimIndent(),
        )
        val offer = response.data!!
        assertNull(offer.endsAt)
        assertEquals("Magicpin", offer.sourceName)
    }

    @Test
    fun `shop offer item parses null ends_at`() {
        val item = json.decodeFromString<ShopOfferItemDto>(
            """
            {
              "offer_id": "fe7d6abd-1d78-45c9-8312-2efc67e0584b",
              "title": "Save 5% on store vouchers",
              "discount_type": "percentage",
              "discount_value": "5.00",
              "starts_at": "2026-09-01T00:00:00Z",
              "ends_at": null,
              "status": "active"
            }
            """.trimIndent(),
        )
        assertNull(item.endsAt)
    }

    @Test
    fun `favorite offer parses null ends_at`() {
        val item = json.decodeFromString<FavoriteOfferDto>(
            """
            {
              "favorite_id": "fav-1",
              "offer_id": "fe7d6abd-1d78-45c9-8312-2efc67e0584b",
              "title": "Save 5% on store vouchers",
              "discount_type": "percentage",
              "discount_value": "5.00",
              "starts_at": "2026-09-01T00:00:00Z",
              "ends_at": null,
              "shop_id": "f85595a3-43f4-4f19-bc7e-f5c2f187d91f",
              "shop_name": "Kirandeep Mobile Stores",
              "is_verified": false,
              "source_type": "EXTERNAL",
              "source_name": "Magicpin",
              "saved_at": "2026-09-01T00:00:00Z"
            }
            """.trimIndent(),
        )
        assertNull(item.endsAt)
        assertEquals("EXTERNAL", item.sourceType)
    }

    @Test
    fun `verified offer with fixed expiry still parses`() {
        val offer = json.decodeFromString<HomeOfferDto>(
            """
            {
              "offer_id": "verified-1",
              "title": "Verified Sale",
              "discount_type": "percentage",
              "discount_value": "10",
              "starts_at": "2026-08-01T00:00:00Z",
              "ends_at": "2026-12-31T23:59:59Z",
              "shop_id": "shop-1",
              "shop_name": "Verified Shop",
              "is_verified": true,
              "source_type": "AASPAS"
            }
            """.trimIndent(),
        )
        assertEquals(true, offer.isVerified)
        assertEquals("2026-12-31T23:59:59Z", offer.endsAt)
        assertEquals("AASPAS", offer.sourceType)
    }

    private fun decodeHomeOffer(): HomeOfferDto {
        val response = json.decodeFromString<ApiResponseDto<HomeDataDto>>(
            """
            {
              "success": true,
              "data": {
                "location": {"available": true},
                "today_offers": [{
                  "offer_id": "fe7d6abd-1d78-45c9-8312-2efc67e0584b",
                  "title": "Save 5% on store vouchers",
                  "discount_type": "percentage",
                  "discount_value": "5.00",
                  "starts_at": "2026-09-01T00:00:00Z",
                  "ends_at": null,
                  "shop_id": "f85595a3-43f4-4f19-bc7e-f5c2f187d91f",
                  "shop_name": "Kirandeep Mobile Stores",
                  "is_verified": false,
                  "source_type": "EXTERNAL",
                  "source_name": "Magicpin"
                }]
              }
            }
            """.trimIndent(),
        )
        return response.data!!.todayOffers.single()
    }
}
