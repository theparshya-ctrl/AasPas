package com.aaspas.customer.presentation.home



import androidx.lifecycle.ViewModel

import androidx.lifecycle.ViewModelProvider

import com.aaspas.customer.domain.repository.AuthRepository

import com.aaspas.customer.domain.repository.FavoritesRepository
import com.aaspas.customer.domain.repository.HomeRepository
import com.aaspas.customer.core.notifications.RealtimeNotificationStore
import com.aaspas.customer.domain.repository.NotificationRepository



class HomeViewModelFactory(
    private val repository: HomeRepository,
    private val favoritesRepository: FavoritesRepository,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val realtimeNotificationStore: RealtimeNotificationStore,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {

            return HomeViewModel(
                repository,
                favoritesRepository,
                authRepository,
                notificationRepository,
                realtimeNotificationStore,
            ) as T

        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")

    }

}


