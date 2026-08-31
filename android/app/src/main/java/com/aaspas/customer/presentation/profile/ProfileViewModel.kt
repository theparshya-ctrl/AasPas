package com.aaspas.customer.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.auth.SessionState
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionState.collect { session ->
                when (session) {
                    SessionState.LoggedOut -> {
                        _uiState.update {
                            ProfileUiState(status = ProfileLoadStatus.Unauthorized)
                        }
                    }
                    SessionState.LoggedIn -> loadProfile()
                }
            }
        }
    }

    fun loadProfile() {
        if (!authRepository.isLoggedIn()) {
            _uiState.update { ProfileUiState(status = ProfileLoadStatus.Unauthorized) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(status = ProfileLoadStatus.Loading, error = null) }
            when (val result = authRepository.getCurrentUser()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(status = ProfileLoadStatus.Loaded, user = result.data, error = null)
                    }
                }
                is Result.Failure -> {
                    val status = if (result.error is AppError.Unauthorized) {
                        ProfileLoadStatus.Unauthorized
                    } else {
                        ProfileLoadStatus.Error
                    }
                    _uiState.update { it.copy(status = status, error = result.error) }
                }
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update { ProfileUiState(status = ProfileLoadStatus.Unauthorized) }
    }
}
