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

class AdminUserManagementViewModel(
    private val repository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUserManagementUiState())
    val uiState: StateFlow<AdminUserManagementUiState> = _uiState.asStateFlow()

    fun load() {
        val query = _uiState.value.searchQuery
        viewModelScope.launch {
            _uiState.update { it.copy(status = AdminUserManagementStatus.Loading, error = null) }
            when (val result = repository.listUsers(search = query)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(status = AdminUserManagementStatus.Loaded, users = result.data)
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(status = AdminUserManagementStatus.Error, error = result.error)
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun submitSearch() {
        load()
    }
}
