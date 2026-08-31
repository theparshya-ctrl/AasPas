package com.aaspas.customer.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aaspas.customer.core.notifications.RealtimeNotificationStore
import com.aaspas.customer.domain.repository.AdminRepository
import com.aaspas.customer.domain.repository.NotificationRepository

class AdminConsoleViewModelFactory(
    private val repository: AdminRepository,
    private val notificationRepository: NotificationRepository,
    private val realtimeNotificationStore: RealtimeNotificationStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminConsoleViewModel::class.java)) {
            return AdminConsoleViewModel(
                repository,
                notificationRepository,
                realtimeNotificationStore,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
