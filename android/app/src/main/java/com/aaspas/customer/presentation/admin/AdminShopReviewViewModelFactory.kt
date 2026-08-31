package com.aaspas.customer.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aaspas.customer.domain.repository.AdminRepository

class AdminShopReviewViewModelFactory(
    private val repository: AdminRepository,
    private val shopId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminShopReviewViewModel::class.java)) {
            return AdminShopReviewViewModel(repository, shopId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
