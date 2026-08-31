package com.aaspas.customer.presentation.shopowner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.core.notifications.RealtimeNotificationStore
import com.aaspas.customer.domain.repository.NotificationRepository
import com.aaspas.customer.domain.repository.ShopOwnerRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShopOwnerDashboardViewModel(
    private val repository: ShopOwnerRepository,
    private val notificationRepository: NotificationRepository,
    private val realtimeNotificationStore: RealtimeNotificationStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopOwnerDashboardUiState())
    val uiState: StateFlow<ShopOwnerDashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            realtimeNotificationStore.unreadCount.collect { count ->
                _uiState.update { it.copy(unreadNotificationCount = count) }
            }
        }
    }

    fun load(preserveCreatedMessage: Boolean = false) {
        viewModelScope.launch {
            val createdMessage = if (preserveCreatedMessage) _uiState.value.createdSuccessMessage else null
            _uiState.update {
                it.copy(
                    status = ShopOwnerLoadStatus.Loading,
                    error = null,
                    submitMessage = null,
                    createdSuccessMessage = createdMessage,
                )
            }
            coroutineScope {
                val dashboardDeferred = async { repository.getDashboard() }
                val notificationsDeferred = async { notificationRepository.listNotifications() }
                when (val result = dashboardDeferred.await()) {
                    is Result.Success -> {
                        val unreadCount = when (val notifications = notificationsDeferred.await()) {
                            is Result.Success -> {
                                realtimeNotificationStore.applyFromRest(notifications.data)
                                notifications.data.unreadCount
                            }
                            is Result.Failure -> 0
                        }
                        _uiState.update {
                            it.copy(
                                status = ShopOwnerLoadStatus.Loaded,
                                dashboard = result.data,
                                error = null,
                                unreadNotificationCount = unreadCount,
                            )
                        }
                    }
                    is Result.Failure -> {
                        _uiState.update {
                            it.copy(
                                status = mapErrorStatus(result.error),
                                dashboard = null,
                                error = result.error,
                                unreadNotificationCount = 0,
                                createdSuccessMessage = null,
                            )
                        }
                    }
                }
            }
        }
    }

    fun refreshNotifications() {
        viewModelScope.launch {
            when (val notifications = notificationRepository.listNotifications()) {
                is Result.Success -> realtimeNotificationStore.applyFromRest(notifications.data)
                is Result.Failure -> Unit
            }
        }
    }

    fun showShopCreatedSuccess(message: String) {
        _uiState.update { it.copy(createdSuccessMessage = message) }
    }

    fun clearCreatedSuccessMessage() {
        _uiState.update { it.copy(createdSuccessMessage = null) }
    }

    fun submitShopForVerification() {
        val shopId = _uiState.value.dashboard?.shop?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(status = ShopOwnerLoadStatus.Submitting, submitMessage = null) }
            when (val result = repository.submitShop(shopId)) {
                is Result.Success -> load()
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            status = ShopOwnerLoadStatus.Loaded,
                            error = result.error,
                            submitMessage = "Could not submit shop for verification.",
                        )
                    }
                }
            }
        }
    }

    private fun mapErrorStatus(error: AppError): ShopOwnerLoadStatus {
        return when (error) {
            AppError.Unauthorized -> ShopOwnerLoadStatus.Unauthorized
            AppError.NotFound -> ShopOwnerLoadStatus.Empty
            else -> ShopOwnerLoadStatus.Error
        }
    }
}
