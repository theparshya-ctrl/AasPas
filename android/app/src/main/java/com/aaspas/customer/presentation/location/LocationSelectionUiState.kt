package com.aaspas.customer.presentation.location

import com.aaspas.customer.core.location.LocationSearchResult
import com.aaspas.customer.domain.model.SelectedLocation

enum class LocationSearchStatus {
    Idle,
    Loading,
    Loaded,
    Empty,
    Error,
}

data class LocationSelectionUiState(
    val searchQuery: String = "",
    val searchStatus: LocationSearchStatus = LocationSearchStatus.Idle,
    val searchResults: List<LocationSearchResult> = emptyList(),
    val searchErrorMessage: String? = null,
    val currentSelection: SelectedLocation = SelectedLocation.None,
    val isApplyingCurrentLocation: Boolean = false,
    val currentLocationError: String? = null,
)
