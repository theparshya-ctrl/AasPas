package com.aaspas.customer.domain.repository

import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.AppNotification
import com.aaspas.customer.domain.model.NotificationList

interface NotificationRepository {
    suspend fun listNotifications(unreadOnly: Boolean = false): Result<NotificationList>
    suspend fun markRead(notificationId: String): Result<AppNotification>
    suspend fun markAllRead(): Result<Int>
}
