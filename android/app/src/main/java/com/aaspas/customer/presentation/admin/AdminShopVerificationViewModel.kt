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

class AdminShopVerificationViewModel(
    private val repository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminShopVerificationUiState())
    val uiState: StateFlow<AdminShopVerificationUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = AdminShopVerificationStatus.Loading, error = null) }
            when (val result = repository.listPendingShops()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(status = AdminShopVerificationStatus.Loaded, shops = result.data)
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(status = AdminShopVerificationStatus.Error, error = result.error)
                    }
                }
            }
        }
    }
}
