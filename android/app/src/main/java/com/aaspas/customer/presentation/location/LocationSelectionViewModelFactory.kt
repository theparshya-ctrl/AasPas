package com.aaspas.customer.presentation.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aaspas.customer.core.location.LocationSearchService
import com.aaspas.customer.core.location.SelectedLocationStore

class LocationSelectionViewModelFactory(
    private val locationSearchService: LocationSearchService,
    private val selectedLocationStore: SelectedLocationStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocationSelectionViewModel::class.java)) {
            return LocationSelectionViewModel(locationSearchService, selectedLocationStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
