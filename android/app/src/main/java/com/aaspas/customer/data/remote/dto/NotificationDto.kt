package com.aaspas.customer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationItemDto(
    val id: String,
    val type: String,
    val audience: String = "customer",
    val title: String,
    val message: String,
    @SerialName("entity_type") val entityType: String? = null,
    @SerialName("entity_id") val entityId: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class NotificationListDto(
    val notifications: List<NotificationItemDto> = emptyList(),
    @SerialName("unread_count") val unreadCount: Int = 0,
)

@Serializable
data class MarkAllReadDto(
    @SerialName("marked_read") val markedRead: Int = 0,
)
