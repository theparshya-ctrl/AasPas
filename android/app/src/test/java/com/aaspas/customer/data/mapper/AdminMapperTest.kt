package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.AdminOfferMerchantDto
import com.aaspas.customer.data.remote.dto.AdminOfferReviewDto
import com.aaspas.customer.data.remote.dto.AdminOfferShopDto
import com.aaspas.customer.data.remote.dto.AdminShopOwnerDto
import com.aaspas.customer.data.remote.dto.AdminShopReviewDto
import com.aaspas.customer.data.remote.dto.BusinessHoursDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AdminMapperTest {

    @Test
    fun `maps pending review dto`() {
        val review = AdminMapper.toReview(
            AdminOfferReviewDto(
                id = "offer-1",
                shopId = "shop-1",
                title = "Weekend Sale",
                description = "Valid on all items",
                discountType = "percentage",
                discountValue = "10",
                status = "pending_approval",
                startsAt = "2026-08-21T09:00:00Z",
                endsAt = "2026-08-21T21:00:00Z",
                submittedAt = "2026-08-20T10:00:00Z",
                merchantConfirmedAt = "2026-08-20T10:00:00Z",
                isVerified = false,
                shop = AdminOfferShopDto(
                    shopId = "shop-1",
                    shopName = "Fresh Mart",
                    category = "grocery",
                    status = "active",
                    isVerified = true,
                ),
                merchant = AdminOfferMerchantDto(
                    userId = "user-1",
                    fullName = "Owner",
                    email = "owner@example.com",
                ),
            ),
        )

        assertEquals("pending_approval", review.status)
        assertEquals("Fresh Mart", review.shop.shopName)
        assertFalse(review.isVerified)
    }

    @Test
    fun `preview offer is not verified before approval`() {
        val review = AdminMapper.toReview(
            AdminOfferReviewDto(
                id = "offer-1",
                shopId = "shop-1",
                title = "Sale",
                discountType = "percentage",
                discountValue = "10",
                status = "pending_approval",
                shop = AdminOfferShopDto("shop-1", "Fresh Mart", status = "active"),
                merchant = AdminOfferMerchantDto("user-1", email = "owner@example.com"),
            ),
        )

        assertFalse(AdminMapper.toPreviewOffer(review).isVerified)
    }

    @Test
    fun `maps pending shop review dto`() {
        val review = AdminMapper.toShopReview(
            AdminShopReviewDto(
                shopId = "shop-1",
                shopName = "Fresh Mart",
                description = "Neighborhood grocery",
                category = "grocery",
                status = "pending_approval",
                submittedAt = "2026-08-20T10:00:00Z",
                isVerified = false,
                addressLine1 = "123 Main Road",
                area = "Mumbai, MH",
                city = "Mumbai",
                pincode = "400001",
                latitude = 19.07609,
                longitude = 72.877426,
                businessHours = BusinessHoursDto("09:00", "21:00"),
                owner = AdminShopOwnerDto(
                    userId = "user-1",
                    fullName = "Owner",
                    email = "owner@example.com",
                    phone = "+919876543210",
                ),
            ),
        )

        assertEquals("pending_approval", review.status)
        assertEquals("Fresh Mart", review.shopName)
        assertEquals("owner@example.com", review.owner.email)
        assertFalse(review.isVerified)
    }
}
