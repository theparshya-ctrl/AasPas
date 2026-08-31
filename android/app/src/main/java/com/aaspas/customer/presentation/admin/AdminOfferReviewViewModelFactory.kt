package com.aaspas.customer.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aaspas.customer.domain.repository.AdminRepository

class AdminOfferReviewViewModelFactory(
    private val repository: AdminRepository,
    private val offerId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminOfferReviewViewModel::class.java)) {
            return AdminOfferReviewViewModel(repository, offerId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
