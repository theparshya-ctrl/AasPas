package com.aaspas.customer.presentation.admin

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.AdminDashboard

enum class AdminConsoleStatus {
    Loading,
    Loaded,
    Unauthorized,
    Error,
}

data class AdminConsoleUiState(
    val status: AdminConsoleStatus = AdminConsoleStatus.Loading,
    val dashboard: AdminDashboard? = null,
    val error: AppError? = null,
    val unreadNotificationCount: Int = 0,
)
