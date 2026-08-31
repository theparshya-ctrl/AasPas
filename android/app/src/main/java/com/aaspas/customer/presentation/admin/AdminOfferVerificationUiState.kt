package com.aaspas.customer.presentation.admin

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.AdminOfferReview

enum class AdminVerificationStatus {
    Loading,
    Loaded,
    Error,
}

data class AdminOfferVerificationUiState(
    val status: AdminVerificationStatus = AdminVerificationStatus.Loading,
    val offers: List<AdminOfferReview> = emptyList(),
    val error: AppError? = null,
)
