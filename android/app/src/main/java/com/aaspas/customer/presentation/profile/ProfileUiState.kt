package com.aaspas.customer.presentation.profile

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.UserAccount

enum class ProfileLoadStatus {
    Loading,
    Loaded,
    Unauthorized,
    Error,
}

data class ProfileUiState(
    val status: ProfileLoadStatus = ProfileLoadStatus.Loading,
    val user: UserAccount? = null,
    val error: AppError? = null,
)
