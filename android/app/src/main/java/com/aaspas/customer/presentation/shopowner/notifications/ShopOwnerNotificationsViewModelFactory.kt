package com.aaspas.customer.presentation.shopowner.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aaspas.customer.domain.repository.NotificationRepository
import com.aaspas.customer.domain.repository.ShopOwnerRepository

class ShopOwnerNotificationsViewModelFactory(
    private val notificationRepository: NotificationRepository,
    private val shopOwnerRepository: ShopOwnerRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopOwnerNotificationsViewModel::class.java)) {
            return ShopOwnerNotificationsViewModel(notificationRepository, shopOwnerRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
