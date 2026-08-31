package com.aaspas.customer.core.notifications

import com.aaspas.customer.data.remote.dto.NotificationItemDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RealtimeNotificationMessage(
    val type: String,
    val notification: NotificationItemDto? = null,
    @SerialName("unread_count") val unreadCount: Int? = null,
)
