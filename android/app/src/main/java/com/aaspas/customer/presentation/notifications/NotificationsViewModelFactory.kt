package com.aaspas.customer.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aaspas.customer.domain.repository.AuthRepository
import com.aaspas.customer.domain.repository.NotificationRepository
import com.aaspas.customer.domain.repository.ShopOwnerRepository

class NotificationsViewModelFactory(
    private val notificationRepository: NotificationRepository,
    private val shopOwnerRepository: ShopOwnerRepository,
    private val authRepository: AuthRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            return NotificationsViewModel(notificationRepository, shopOwnerRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
