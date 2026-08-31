package com.aaspas.customer.presentation.shopowner



import androidx.lifecycle.ViewModel

import androidx.lifecycle.ViewModelProvider

import com.aaspas.customer.core.notifications.RealtimeNotificationStore
import com.aaspas.customer.domain.repository.NotificationRepository

import com.aaspas.customer.domain.repository.ShopOwnerRepository



class ShopOwnerDashboardViewModelFactory(

    private val repository: ShopOwnerRepository,

    private val notificationRepository: NotificationRepository,

    private val realtimeNotificationStore: RealtimeNotificationStore,

) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(ShopOwnerDashboardViewModel::class.java)) {

            return ShopOwnerDashboardViewModel(
                repository,
                notificationRepository,
                realtimeNotificationStore,
            ) as T

        }

        throw IllegalArgumentException("Unknown ViewModel class")

    }

}

