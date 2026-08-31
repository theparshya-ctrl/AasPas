package com.aaspas.customer.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun submit() {
        val state = _uiState.value
        val email = state.email.trim()
        val password = state.password
        val confirm = state.confirmPassword

        val validationError = when {
            email.isBlank() || !email.contains("@") -> "Enter a valid email address."
            password.length < 8 -> "Password must be at least 8 characters."
            password != confirm -> "Passwords do not match."
            else -> null
        }

        if (validationError != null) {
            _uiState.update {
                it.copy(status = RegisterStatus.ValidationError, errorMessage = validationError)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(status = RegisterStatus.Loading, error = null, errorMessage = null) }
            when (
                val result = authRepository.register(
                    email = email,
                    password = password,
                    fullName = state.fullName.trim().takeIf { it.isNotBlank() },
                )
            ) {
                is Result.Success -> {
                    _uiState.update { it.copy(status = RegisterStatus.Success) }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            status = RegisterStatus.Error,
                            error = result.error,
                            errorMessage = mapErrorMessage(result.error),
                        )
                    }
                }
            }
        }
    }

    private fun mapErrorMessage(error: AppError): String {
        return when (error) {
            AppError.Unauthorized -> "Registration failed. Please try again."
            AppError.NoInternet -> "Please check your internet connection."
            AppError.Timeout -> "The request timed out. Please try again."
            is AppError.Unexpected -> error.detail
            else -> error.message
        }
    }
}
