package com.aaspas.customer.presentation.notifications

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.AppNotification

enum class NotificationsLoadStatus {
    Loading,
    Loaded,
    Empty,
    Error,
}

enum class NotificationFilter {
    ALL,
    BUSINESS,
    CUSTOMER,
}

data class NotificationsUiState(
    val status: NotificationsLoadStatus = NotificationsLoadStatus.Loading,
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val error: AppError? = null,
    val navigationTarget: NotificationNavigationTarget? = null,
    val filter: NotificationFilter = NotificationFilter.ALL,
    val userRole: String? = null,
    val showBusinessFilter: Boolean = false,
    val showCustomerFilter: Boolean = true,
)
