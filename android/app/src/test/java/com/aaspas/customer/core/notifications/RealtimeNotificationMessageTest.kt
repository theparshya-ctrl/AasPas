package com.aaspas.customer.core.notifications

import com.aaspas.customer.data.mapper.NotificationMapper
import com.aaspas.customer.data.remote.dto.NotificationItemDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RealtimeNotificationMessageTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `parses websocket notification payload`() {
        val payload = """
            {
              "type": "notification",
              "notification": {
                "id": "abc",
                "type": "SHOP_APPROVED",
                "audience": "business",
                "title": "Shop approved",
                "message": "Your shop has been approved.",
                "entity_type": "shop",
                "entity_id": "shop-1",
                "is_read": false,
                "created_at": "2026-01-01T00:00:00Z"
              },
              "unread_count": 3
            }
        """.trimIndent()

        val message = json.decodeFromString<RealtimeNotificationMessage>(payload)
        assertEquals("notification", message.type)
        assertEquals(3, message.unreadCount)
        val notification = NotificationMapper.toDomain(message.notification!!)
        assertEquals("SHOP_APPROVED", notification.type)
        assertNotNull(notification.entityId)
    }
}
