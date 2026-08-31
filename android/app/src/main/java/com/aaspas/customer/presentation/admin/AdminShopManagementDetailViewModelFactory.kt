package com.aaspas.customer.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aaspas.customer.domain.repository.AdminRepository

class AdminShopManagementDetailViewModelFactory(
    private val repository: AdminRepository,
    private val shopId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminShopManagementDetailViewModel::class.java)) {
            return AdminShopManagementDetailViewModel(repository, shopId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
