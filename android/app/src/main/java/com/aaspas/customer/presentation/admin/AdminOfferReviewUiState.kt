package com.aaspas.customer.presentation.admin

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.AdminOfferReview

enum class AdminOfferReviewStatus {
    Loading,
    Loaded,
    Approving,
    Rejecting,
    Approved,
    Rejected,
    Error,
}

data class AdminOfferReviewUiState(
    val status: AdminOfferReviewStatus = AdminOfferReviewStatus.Loading,
    val review: AdminOfferReview? = null,
    val showApproveDialog: Boolean = false,
    val showRejectDialog: Boolean = false,
    val rejectReason: String = "",
    val selectedRejectPreset: String? = null,
    val validationMessage: String? = null,
    val error: AppError? = null,
)
