package com.aaspas.customer.presentation.shopowner.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.AppNotification
import com.aaspas.customer.domain.model.NotificationEntityType
import com.aaspas.customer.domain.repository.NotificationRepository
import com.aaspas.customer.domain.repository.ShopOwnerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NotificationsLoadStatus {
    Loading,
    Loaded,
    Empty,
    Error,
}

data class NotificationsUiState(
    val status: NotificationsLoadStatus = NotificationsLoadStatus.Loading,
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val error: AppError? = null,
    val navigationTarget: NotificationNavigationTarget? = null,
)

class ShopOwnerNotificationsViewModel(
    private val notificationRepository: NotificationRepository,
    private val shopOwnerRepository: ShopOwnerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = NotificationsLoadStatus.Loading, error = null) }
            when (val result = notificationRepository.listNotifications()) {
                is Result.Success -> {
                    val items = result.data.notifications
                    _uiState.update {
                        it.copy(
                            status = if (items.isEmpty()) NotificationsLoadStatus.Empty else NotificationsLoadStatus.Loaded,
                            notifications = items,
                            unreadCount = result.data.unreadCount,
                            error = null,
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            status = NotificationsLoadStatus.Error,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    fun onNotificationClick(notification: AppNotification) {
        viewModelScope.launch {
            notificationRepository.markRead(notification.id)
            var shopId: String? = null
            if (NotificationEntityType.from(notification.entityType) == NotificationEntityType.OFFER) {
                val offerId = notification.entityId
                if (!offerId.isNullOrBlank()) {
                    when (val offer = shopOwnerRepository.getOffer(offerId)) {
                        is Result.Success -> shopId = offer.data.shopId
                        is Result.Failure -> Unit
                    }
                }
            }
            if (shopId == null) {
                when (val dashboard = shopOwnerRepository.getDashboard()) {
                    is Result.Success -> shopId = dashboard.data.shop.id
                    is Result.Failure -> Unit
                }
            }
            val target = NotificationNavigation.resolve(notification, shopId)
            _uiState.update { state ->
                val updated = state.notifications.map { item ->
                    if (item.id == notification.id) item.copy(isRead = true) else item
                }
                state.copy(
                    notifications = updated,
                    unreadCount = updated.count { !it.isRead },
                    navigationTarget = target,
                )
            }
        }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(navigationTarget = null) }
    }
}
