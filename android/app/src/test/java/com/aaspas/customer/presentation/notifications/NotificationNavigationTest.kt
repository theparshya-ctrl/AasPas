package com.aaspas.customer.presentation.notifications

import com.aaspas.customer.core.auth.UserRoles
import com.aaspas.customer.domain.model.AppNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationNavigationTest {
    @Test
    fun `customer shop notification opens shop details`() {
        val target = NotificationNavigation.resolve(
            notification = AppNotification(
                id = "1",
                type = "SHOP_APPROVED",
                audience = "business",
                title = "Shop approved",
                message = "Approved",
                entityType = "shop",
                entityId = "shop-1",
                isRead = false,
                createdAt = "2026-08-26T10:00:00Z",
            ),
            userRole = UserRoles.CUSTOMER,
            shopId = null,
        )
        assertEquals(NotificationNavigationTarget.ShopDetails("shop-1"), target)
    }

    @Test
    fun `shop owner offer rejection opens edit offer`() {
        val target = NotificationNavigation.resolve(
            notification = AppNotification(
                id = "1",
                type = "OFFER_REJECTED",
                audience = "business",
                title = "Offer rejected",
                message = "Reason",
                entityType = "offer",
                entityId = "offer-1",
                isRead = false,
                createdAt = "2026-08-26T10:00:00Z",
            ),
            userRole = UserRoles.SHOP_OWNER,
            shopId = "shop-1",
        )
        assertEquals(NotificationNavigationTarget.EditOffer("shop-1", "offer-1"), target)
    }

    @Test
    fun `customer offer notification opens offer details`() {
        val target = NotificationNavigation.resolve(
            notification = AppNotification(
                id = "1",
                type = "OFFER_APPROVED",
                audience = "customer",
                title = "Offer",
                message = "Nearby offer",
                entityType = "offer",
                entityId = "offer-9",
                isRead = false,
                createdAt = "2026-08-26T10:00:00Z",
            ),
            userRole = UserRoles.CUSTOMER,
            shopId = null,
        )
        assertEquals(NotificationNavigationTarget.OfferDetails("offer-9"), target)
    }

    @Test
    fun `customer favorite shop new offer opens offer details`() {
        val target = NotificationNavigation.resolve(
            notification = AppNotification(
                id = "1",
                type = "CUSTOMER_FAVORITE_SHOP_NEW_OFFER",
                audience = "customer",
                title = "New offer",
                message = "New offer from Demo Fashion Hub",
                entityType = "offer",
                entityId = "offer-42",
                isRead = false,
                createdAt = "2026-08-26T10:00:00Z",
            ),
            userRole = UserRoles.CUSTOMER,
            shopId = null,
        )
        assertEquals(NotificationNavigationTarget.OfferDetails("offer-42"), target)
    }

    @Test
    fun `customer favorite offer active opens offer details`() {
        val target = NotificationNavigation.resolve(
            notification = AppNotification(
                id = "2",
                type = "CUSTOMER_FAVORITE_OFFER_ACTIVE",
                audience = "customer",
                title = "Saved offer active",
                message = "Your saved offer is now active.",
                entityType = "offer",
                entityId = "offer-7",
                isRead = false,
                createdAt = "2026-08-26T10:00:00Z",
            ),
            userRole = UserRoles.CUSTOMER,
            shopId = null,
        )
        assertEquals(NotificationNavigationTarget.OfferDetails("offer-7"), target)
    }

    @Test
    fun `shop owner shop notification opens profile`() {
        val target = NotificationNavigation.resolve(
            notification = AppNotification(
                id = "1",
                type = "SHOP_APPROVED",
                audience = "business",
                title = "Shop approved",
                message = "Approved",
                entityType = "shop",
                entityId = "shop-1",
                isRead = false,
                createdAt = "2026-08-26T10:00:00Z",
            ),
            userRole = UserRoles.SHOP_OWNER,
            shopId = "shop-1",
        )
        assertTrue(target is NotificationNavigationTarget.ShopProfile)
    }
}
