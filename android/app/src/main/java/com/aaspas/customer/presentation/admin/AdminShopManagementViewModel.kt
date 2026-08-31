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

class AdminShopManagementViewModel(
    private val repository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminShopManagementUiState())
    val uiState: StateFlow<AdminShopManagementUiState> = _uiState.asStateFlow()

    fun load() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(status = AdminShopManagementStatus.Loading, error = null) }
            when (
                val result = repository.listShops(
                    status = state.filter.apiStatus(),
                    search = state.searchQuery,
                )
            ) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(status = AdminShopManagementStatus.Loaded, shops = result.data)
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(status = AdminShopManagementStatus.Error, error = result.error)
                    }
                }
            }
        }
    }

    fun onFilterSelected(filter: AdminShopFilter) {
        _uiState.update { it.copy(filter = filter) }
        load()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun submitSearch() {
        load()
    }
}

private fun AdminShopFilter.apiStatus(): String? = when (this) {
    AdminShopFilter.ALL -> null
    AdminShopFilter.PENDING -> "pending_approval"
    AdminShopFilter.ACTIVE -> "active"
    AdminShopFilter.REJECTED -> "rejected"
}
