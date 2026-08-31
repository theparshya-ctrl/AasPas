package com.aaspas.customer.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aaspas.customer.domain.repository.AdminRepository

class AdminOfferVerificationViewModelFactory(
    private val repository: AdminRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminOfferVerificationViewModel::class.java)) {
            return AdminOfferVerificationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
