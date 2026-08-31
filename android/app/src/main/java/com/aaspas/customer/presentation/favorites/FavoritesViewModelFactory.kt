package com.aaspas.customer.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aaspas.customer.domain.repository.AuthRepository
import com.aaspas.customer.domain.repository.FavoritesRepository

class FavoritesViewModelFactory(
    private val favoritesRepository: FavoritesRepository,
    private val authRepository: AuthRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoritesViewModel::class.java)) {
            return FavoritesViewModel(favoritesRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
