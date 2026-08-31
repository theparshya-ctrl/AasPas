package com.aaspas.customer.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.AdminShopDetail
import com.aaspas.customer.domain.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminShopManagementDetailUiState(
    val status: AdminShopManagementDetailStatus = AdminShopManagementDetailStatus.Loading,
    val detail: AdminShopDetail? = null,
    val error: AppError? = null,
)

class AdminShopManagementDetailViewModel(
    private val repository: AdminRepository,
    private val shopId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminShopManagementDetailUiState())
    val uiState: StateFlow<AdminShopManagementDetailUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = AdminShopManagementDetailStatus.Loading, error = null) }
            when (val result = repository.getShopDetail(shopId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(status = AdminShopManagementDetailStatus.Loaded, detail = result.data)
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(status = AdminShopManagementDetailStatus.Error, error = result.error)
                    }
                }
            }
        }
    }
}
