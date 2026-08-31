package com.aaspas.customer.presentation.admin

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.AdminUserListItem

enum class AdminUserManagementStatus {
    Loading,
    Loaded,
    Error,
}

data class AdminUserManagementUiState(
    val status: AdminUserManagementStatus = AdminUserManagementStatus.Loading,
    val users: List<AdminUserListItem> = emptyList(),
    val searchQuery: String = "",
    val error: AppError? = null,
)
