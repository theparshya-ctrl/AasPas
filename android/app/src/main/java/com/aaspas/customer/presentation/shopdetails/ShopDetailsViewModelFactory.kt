package com.aaspas.customer.presentation.shopdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aaspas.customer.domain.repository.ShopDetailsRepository

class ShopDetailsViewModelFactory(
    private val repository: ShopDetailsRepository,
    private val shopId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopDetailsViewModel::class.java)) {
            return ShopDetailsViewModel(repository, shopId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
