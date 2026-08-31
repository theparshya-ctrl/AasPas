package com.aaspas.customer.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.auth.RoleAccess
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.AppNotification
import com.aaspas.customer.domain.model.NotificationAudience
import com.aaspas.customer.domain.model.NotificationEntityType
import com.aaspas.customer.domain.repository.AuthRepository
import com.aaspas.customer.domain.repository.NotificationRepository
import com.aaspas.customer.domain.repository.ShopOwnerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository,
    private val shopOwnerRepository: ShopOwnerRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = NotificationsLoadStatus.Loading, error = null) }
            val role = when (val user = authRepository.getCurrentUser()) {
                is Result.Success -> user.data.role
                is Result.Failure -> null
            }
            when (val result = notificationRepository.listNotifications()) {
                is Result.Success -> {
                    val items = result.data.notifications
                    _uiState.update {
                        it.copy(
                            status = if (items.isEmpty()) {
                                NotificationsLoadStatus.Empty
                            } else {
                                NotificationsLoadStatus.Loaded
                            },
                            notifications = items,
                            unreadCount = result.data.unreadCount,
                            error = null,
                            userRole = role,
                            showBusinessFilter = RoleAccess.canAccessShopOwner(role),
                            showCustomerFilter = true,
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(status = NotificationsLoadStatus.Error, error = result.error)
                    }
                }
            }
        }
    }

    fun setFilter(filter: NotificationFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun filteredNotifications(): List<AppNotification> {
        val state = _uiState.value
        return when (state.filter) {
            NotificationFilter.ALL -> state.notifications
            NotificationFilter.BUSINESS -> state.notifications.filter {
                NotificationAudience.from(it.audience) == NotificationAudience.BUSINESS
            }
            NotificationFilter.CUSTOMER -> state.notifications.filter {
                NotificationAudience.from(it.audience) == NotificationAudience.CUSTOMER
            }
        }
    }

    fun onNotificationClick(notification: AppNotification) {
        viewModelScope.launch {
            notificationRepository.markRead(notification.id)
            var shopId: String? = null
            if (NotificationEntityType.from(notification.entityType) == NotificationEntityType.OFFER &&
                RoleAccess.canAccessShopOwner(_uiState.value.userRole)
            ) {
                val offerId = notification.entityId
                if (!offerId.isNullOrBlank()) {
                    when (val offer = shopOwnerRepository.getOffer(offerId)) {
                        is Result.Success -> shopId = offer.data.shopId
                        is Result.Failure -> Unit
                    }
                }
            }
            if (shopId == null && RoleAccess.canAccessShopOwner(_uiState.value.userRole)) {
                when (val dashboard = shopOwnerRepository.getDashboard()) {
                    is Result.Success -> shopId = dashboard.data.shop.id
                    is Result.Failure -> Unit
                }
            }
            val target = NotificationNavigation.resolve(notification, _uiState.value.userRole, shopId)
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
