package com.aaspas.customer.presentation.shopowner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aaspas.customer.domain.repository.ShopOwnerRepository

class ManageOffersViewModelFactory(
    private val repository: ShopOwnerRepository,
    private val shopId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageOffersViewModel::class.java)) {
            return ManageOffersViewModel(repository, shopId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
