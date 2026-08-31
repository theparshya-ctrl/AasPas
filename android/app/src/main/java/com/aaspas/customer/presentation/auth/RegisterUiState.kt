package com.aaspas.customer.presentation.auth

import com.aaspas.customer.core.common.AppError

enum class RegisterStatus {
    Idle,
    Loading,
    Success,
    ValidationError,
    Error,
}

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val status: RegisterStatus = RegisterStatus.Idle,
    val error: AppError? = null,
    val errorMessage: String? = null,
)
