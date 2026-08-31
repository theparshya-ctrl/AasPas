package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.NotificationItemDto
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationMapperTest {
    @Test
    fun `maps audience from dto`() {
        val notification = NotificationMapper.toDomain(
            NotificationItemDto(
                id = "n1",
                type = "SHOP_APPROVED",
                audience = "business",
                title = "Shop approved",
                message = "Approved",
                entityType = "shop",
                entityId = "shop-1",
                isRead = false,
                createdAt = "2026-08-26T10:00:00Z",
            ),
        )
        assertEquals("business", notification.audience)
    }
}
