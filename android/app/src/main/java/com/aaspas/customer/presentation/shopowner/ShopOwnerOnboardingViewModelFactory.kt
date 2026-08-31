package com.aaspas.customer.presentation.shopowner

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aaspas.customer.domain.repository.ShopOwnerRepository

class ShopOwnerOnboardingViewModelFactory(
    private val application: Application,
    private val repository: ShopOwnerRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopOwnerOnboardingViewModel::class.java)) {
            return ShopOwnerOnboardingViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
