package com.aaspas.customer.presentation.auth

import com.aaspas.customer.core.common.AppError

enum class LoginStatus {
    Idle,
    Loading,
    Success,
    Error,
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val status: LoginStatus = LoginStatus.Idle,
    val loggedInRole: String? = null,
    val error: AppError? = null,
    val errorMessage: String? = null,
)
