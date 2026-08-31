package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.BusinessHoursDto
import com.aaspas.customer.data.remote.dto.OfferStatusCountsDto
import com.aaspas.customer.data.remote.dto.ShopOwnerDashboardDto
import com.aaspas.customer.data.remote.dto.ShopOwnerLocationDto
import com.aaspas.customer.data.remote.dto.ShopOwnerShopDetailDto
import com.aaspas.customer.domain.model.MerchantOfferStatus
import com.aaspas.customer.domain.model.MerchantShopStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopOwnerMapperTest {

    @Test
    fun `maps dashboard dto to domain`() {
        val domain = ShopOwnerMapper.toDomain(sampleDashboardDto())

        assertEquals("Fresh Mart", domain.shop.name)
        assertTrue(domain.canEditProfile)
        assertFalse(domain.isVerified)
        assertEquals(2, domain.offerCounts.draft)
        assertEquals(1, domain.offerCounts.active)
        assertEquals("Your shop is awaiting approval.", domain.statusMessage)
    }

    @Test
    fun `maps offer status values`() {
        assertEquals(MerchantOfferStatus.Draft, ShopOwnerMapper.mapOfferStatus("draft"))
        assertEquals(MerchantOfferStatus.PendingApproval, ShopOwnerMapper.mapOfferStatus("pending_approval"))
        assertEquals(MerchantOfferStatus.Rejected, ShopOwnerMapper.mapOfferStatus("rejected"))
        assertEquals(MerchantOfferStatus.Scheduled, ShopOwnerMapper.mapOfferStatus("scheduled"))
    }

    @Test
    fun `maps merchant offer dto to domain`() {
        val offer = ShopOwnerMapper.toOffer(
            com.aaspas.customer.data.remote.dto.MerchantOfferDto(
                id = "offer-1",
                shopId = "shop-1",
                title = "10% Off",
                description = "Weekend only",
                discountType = "percentage",
                discountValue = "10",
                status = "pending_approval",
                startsAt = "2026-08-21T09:00:00Z",
                endsAt = "2026-08-21T21:00:00Z",
                photoUrl = null,
                applicableProducts = null,
                minPurchaseAmount = null,
                terms = null,
                rejectionReason = null,
                isVerified = false,
                merchantConfirmedAt = "2026-08-20T10:00:00Z",
                submittedAt = "2026-08-20T10:00:00Z",
                approvedAt = null,
                rejectedAt = null,
                createdAt = "2026-08-20T09:00:00Z",
                updatedAt = "2026-08-20T10:00:00Z",
            ),
        )

        assertEquals(MerchantOfferStatus.PendingApproval, offer.status)
        assertEquals("10", offer.discountValue)
    }

    @Test
    fun `maps shop status values`() {
        assertEquals(MerchantShopStatus.Active, ShopOwnerMapper.mapShopStatus("active"))
        assertEquals(MerchantShopStatus.PendingApproval, ShopOwnerMapper.mapShopStatus("pending_approval"))
        assertEquals(MerchantShopStatus.Unknown, ShopOwnerMapper.mapShopStatus("suspended"))
    }

    @Test
    fun `maps create dto from domain input`() {
        val dto = ShopOwnerMapper.toCreateDto(
            name = "Fresh Mart",
            category = "grocery",
            description = "Neighborhood grocery",
            contactNumber = "+919876543210",
            photoUrl = null,
            businessHours = com.aaspas.customer.domain.model.MerchantBusinessHours("09:00", "21:00"),
            location = com.aaspas.customer.domain.model.MerchantShopLocationDraft(
                addressLine1 = "123 Main Road",
                city = "Mumbai",
                latitude = "19.07609",
                longitude = "72.877426",
            ),
        )

        assertEquals("Fresh Mart", dto.name)
        assertEquals("grocery", dto.category)
        assertEquals("123 Main Road", dto.address.addressLine1)
        assertEquals("19.07609", dto.address.latitude)
        assertEquals("09:00", dto.businessHours.opensAt)
    }

    @Test
    fun `maps location draft from profile`() {
        val profile = ShopOwnerMapper.toShopProfile(sampleDashboardDto().shop)
        val draft = ShopOwnerMapper.locationDraftFromProfile(profile.location)

        assertEquals("123 Main Road", draft.addressLine1)
        assertEquals("19.07609", draft.latitude)
        assertEquals("72.877426", draft.longitude)
    }

    private fun sampleDashboardDto() = ShopOwnerDashboardDto(
        shop = ShopOwnerShopDetailDto(
            id = "shop-1",
            ownerId = "owner-1",
            name = "Fresh Mart",
            slug = "fresh-mart",
            description = "Neighborhood grocery",
            category = "grocery",
            contactNumber = "+919876543210",
            businessHours = BusinessHoursDto("09:00", "21:00"),
            photoUrl = null,
            status = "draft",
            rejectionReason = null,
            submittedAt = null,
            approvedAt = null,
            createdAt = "2025-08-19T10:00:00Z",
            updatedAt = "2025-08-19T10:00:00Z",
            primaryLocation = ShopOwnerLocationDto(
                id = "loc-1",
                shopId = "shop-1",
                label = "Primary",
                addressLine1 = "123 Main Road",
                addressLine2 = null,
                city = "Mumbai",
                state = "MH",
                postalCode = "400001",
                country = "IN",
                latitude = "19.07609",
                longitude = "72.877426",
                isPrimary = true,
                createdAt = "2025-08-19T10:00:00Z",
            ),
        ),
        isVerified = false,
        canEditProfile = true,
        offerCounts = OfferStatusCountsDto(draft = 2, active = 1),
        statusMessage = "Your shop is awaiting approval.",
    )
}
