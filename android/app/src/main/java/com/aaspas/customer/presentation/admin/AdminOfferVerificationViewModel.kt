package com.aaspas.customer.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminOfferVerificationViewModel(
    private val repository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminOfferVerificationUiState())
    val uiState: StateFlow<AdminOfferVerificationUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = AdminVerificationStatus.Loading, error = null) }
            when (val result = repository.listPendingOffers()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(status = AdminVerificationStatus.Loaded, offers = result.data)
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(status = AdminVerificationStatus.Error, error = result.error)
                    }
                }
            }
        }
    }
}
