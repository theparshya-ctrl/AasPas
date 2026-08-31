package com.aaspas.customer.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aaspas.customer.domain.repository.HomeRepository

class MapViewModelFactory(
    private val homeRepository: HomeRepository,
    private val googleMapsKeyConfigured: Boolean,
    private val categoryId: String? = null,
    private val categoryName: String? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(
                homeRepository,
                googleMapsKeyConfigured,
                categoryId,
                categoryName,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
