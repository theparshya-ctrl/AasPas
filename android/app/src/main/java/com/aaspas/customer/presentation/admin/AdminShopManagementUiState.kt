package com.aaspas.customer.presentation.admin

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.AdminShopListItem

enum class AdminShopManagementStatus {
    Loading,
    Loaded,
    Error,
}

enum class AdminShopFilter {
    ALL,
    PENDING,
    ACTIVE,
    REJECTED,
}

data class AdminShopManagementUiState(
    val status: AdminShopManagementStatus = AdminShopManagementStatus.Loading,
    val shops: List<AdminShopListItem> = emptyList(),
    val filter: AdminShopFilter = AdminShopFilter.ALL,
    val searchQuery: String = "",
    val error: AppError? = null,
)

enum class AdminShopManagementDetailStatus {
    Loading,
    Loaded,
    Error,
}
