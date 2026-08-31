package com.aaspas.customer.core.notifications

import com.aaspas.customer.domain.model.AppNotification
import com.aaspas.customer.domain.model.NotificationList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeNotificationStoreTest {

    private val sampleNotification = AppNotification(
        id = "n-1",
        type = "SHOP_APPROVED",
        audience = "business",
        title = "Shop approved",
        message = "Your shop has been approved.",
        entityType = "shop",
        entityId = "shop-1",
        isRead = false,
        createdAt = "2026-01-01T00:00:00Z",
    )

    @Test
    fun `applyFromRest sets unread count and tracks ids`() {
        val store = RealtimeNotificationStore()
        store.applyFromRest(NotificationList(listOf(sampleNotification), unreadCount = 4))
        assertEquals(4, store.unreadCount.value)
    }

    @Test
    fun `applyFromWebSocket updates unread count`() {
        val store = RealtimeNotificationStore()
        assertTrue(store.applyFromWebSocket(sampleNotification, unreadCount = 1))
        assertEquals(1, store.unreadCount.value)
    }

    @Test
    fun `duplicate notification id is ignored for banner`() {
        val store = RealtimeNotificationStore()
        assertTrue(store.applyFromWebSocket(sampleNotification, unreadCount = 1))
        assertFalse(store.applyFromWebSocket(sampleNotification, unreadCount = 1))
        assertEquals(1, store.unreadCount.value)
    }

    @Test
    fun `reset clears unread count`() {
        val store = RealtimeNotificationStore()
        store.applyFromRest(NotificationList(listOf(sampleNotification), unreadCount = 2))
        store.reset()
        assertEquals(0, store.unreadCount.value)
    }
}
