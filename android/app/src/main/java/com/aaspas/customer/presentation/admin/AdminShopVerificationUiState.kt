package com.aaspas.customer.presentation.admin

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.AdminShopReview

enum class AdminShopVerificationStatus {
    Loading,
    Loaded,
    Error,
}

data class AdminShopVerificationUiState(
    val status: AdminShopVerificationStatus = AdminShopVerificationStatus.Loading,
    val shops: List<AdminShopReview> = emptyList(),
    val error: AppError? = null,
)
