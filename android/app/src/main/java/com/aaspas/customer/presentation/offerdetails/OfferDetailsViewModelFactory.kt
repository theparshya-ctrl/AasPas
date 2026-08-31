package com.aaspas.customer.presentation.offerdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aaspas.customer.domain.repository.OfferDetailsRepository

class OfferDetailsViewModelFactory(
    private val repository: OfferDetailsRepository,
    private val offerId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OfferDetailsViewModel::class.java)) {
            return OfferDetailsViewModel(repository, offerId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
