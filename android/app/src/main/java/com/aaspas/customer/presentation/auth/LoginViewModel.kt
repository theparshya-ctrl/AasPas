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

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null, errorMessage = null) }
    }

    fun submit() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        if (email.isBlank() || password.length < 8) {
            _uiState.update {
                it.copy(
                    status = LoginStatus.Error,
                    errorMessage = "Enter a valid email and password (min 8 characters).",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(status = LoginStatus.Loading, error = null, errorMessage = null) }
            when (val result = authRepository.login(email, password)) {
                is Result.Success -> {
                    val role = when (val userResult = authRepository.getCurrentUser()) {
                        is Result.Success -> userResult.data.role
                        is Result.Failure -> null
                    }
                    _uiState.update {
                        it.copy(status = LoginStatus.Success, loggedInRole = role)
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            status = LoginStatus.Error,
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
            AppError.Unauthorized -> "Invalid email or password."
            AppError.NoInternet -> "Please check your internet connection."
            AppError.Timeout -> "The request timed out. Please try again."
            AppError.Server -> "Something went wrong. Please try again."
            else -> error.message
        }
    }
}
