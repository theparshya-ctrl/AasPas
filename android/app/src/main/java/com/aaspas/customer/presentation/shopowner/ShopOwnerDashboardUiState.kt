package com.aaspas.customer.presentation.shopowner

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.ShopOwnerDashboard

enum class ShopOwnerLoadStatus {
    Loading,
    Loaded,
    Submitting,
    Empty,
    Error,
    Unauthorized,
}

data class ShopOwnerDashboardUiState(
    val status: ShopOwnerLoadStatus = ShopOwnerLoadStatus.Loading,
    val dashboard: ShopOwnerDashboard? = null,
    val error: AppError? = null,
    val submitMessage: String? = null,
    val createdSuccessMessage: String? = null,
    val unreadNotificationCount: Int = 0,
)
