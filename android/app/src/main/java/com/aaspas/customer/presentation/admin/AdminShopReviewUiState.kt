package com.aaspas.customer.presentation.admin

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.AdminShopReview

enum class AdminShopReviewStatus {
    Loading,
    Loaded,
    Approving,
    Rejecting,
    Approved,
    Rejected,
    Error,
}

data class AdminShopReviewUiState(
    val status: AdminShopReviewStatus = AdminShopReviewStatus.Loading,
    val review: AdminShopReview? = null,
    val error: AppError? = null,
    val showApproveDialog: Boolean = false,
    val showRejectDialog: Boolean = false,
    val rejectReason: String = "",
    val selectedRejectPreset: String? = null,
    val validationMessage: String? = null,
)
