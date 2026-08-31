package com.aaspas.customer.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.core.notifications.RealtimeNotificationStore
import com.aaspas.customer.domain.repository.AdminRepository
import com.aaspas.customer.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminConsoleViewModel(
    private val repository: AdminRepository,
    private val notificationRepository: NotificationRepository,
    private val realtimeNotificationStore: RealtimeNotificationStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminConsoleUiState())
    val uiState: StateFlow<AdminConsoleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            realtimeNotificationStore.unreadCount.collect { count ->
                _uiState.update { it.copy(unreadNotificationCount = count) }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = AdminConsoleStatus.Loading, error = null) }
            when (val result = repository.getDashboard()) {
                is Result.Success -> {
                    val unreadCount = when (val notifications = notificationRepository.listNotifications()) {
                        is Result.Success -> {
                            realtimeNotificationStore.applyFromRest(notifications.data)
                            notifications.data.unreadCount
                        }
                        is Result.Failure -> 0
                    }
                    _uiState.update {
                        it.copy(
                            status = AdminConsoleStatus.Loaded,
                            dashboard = result.data,
                            unreadNotificationCount = unreadCount,
                        )
                    }
                }
                is Result.Failure -> {
                    val status = if (result.error == AppError.Unauthorized) {
                        AdminConsoleStatus.Unauthorized
                    } else {
                        AdminConsoleStatus.Error
                    }
                    _uiState.update { it.copy(status = status, error = result.error) }
                }
            }
        }
    }
}
