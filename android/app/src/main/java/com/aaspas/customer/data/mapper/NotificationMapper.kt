package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.NotificationItemDto
import com.aaspas.customer.data.remote.dto.NotificationListDto
import com.aaspas.customer.domain.model.AppNotification
import com.aaspas.customer.domain.model.NotificationList

object NotificationMapper {
    fun toDomain(dto: NotificationListDto): NotificationList {
        return NotificationList(
            notifications = dto.notifications.map(::toDomain),
            unreadCount = dto.unreadCount,
        )
    }

    fun toDomain(dto: NotificationItemDto): AppNotification {
        return AppNotification(
            id = dto.id,
            type = dto.type,
            audience = dto.audience,
            title = dto.title,
            message = dto.message,
            entityType = dto.entityType,
            entityId = dto.entityId,
            isRead = dto.isRead,
            createdAt = dto.createdAt,
        )
    }
}
