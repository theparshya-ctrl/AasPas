package com.aaspas.customer.presentation.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.location.LocationSearchOutcome
import com.aaspas.customer.core.location.LocationSearchService
import com.aaspas.customer.core.location.SelectedLocationStore
import com.aaspas.customer.domain.model.LocationSource
import com.aaspas.customer.domain.model.SelectedLocation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationSelectionViewModel(
    private val locationSearchService: LocationSearchService,
    private val selectedLocationStore: SelectedLocationStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LocationSelectionUiState(currentSelection = selectedLocationStore.current()),
    )
    val uiState: StateFlow<LocationSelectionUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            selectedLocationStore.selectedLocation.collect { selection ->
                _uiState.update { it.copy(currentSelection = selection) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                searchErrorMessage = null,
            )
        }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update {
                it.copy(
                    searchStatus = LocationSearchStatus.Idle,
                    searchResults = emptyList(),
                )
            }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update {
                it.copy(
                    searchStatus = LocationSearchStatus.Loading,
                    searchErrorMessage = null,
                )
            }
            when (val outcome = locationSearchService.search(query)) {
                is LocationSearchOutcome.Success -> {
                    _uiState.update {
                        it.copy(
                            searchStatus = LocationSearchStatus.Loaded,
                            searchResults = outcome.results,
                            searchErrorMessage = null,
                        )
                    }
                }
                LocationSearchOutcome.Empty -> {
                    _uiState.update {
                        it.copy(
                            searchStatus = LocationSearchStatus.Empty,
                            searchResults = emptyList(),
                        )
                    }
                }
                LocationSearchOutcome.GeocoderUnavailable -> {
                    _uiState.update {
                        it.copy(
                            searchStatus = LocationSearchStatus.Error,
                            searchResults = emptyList(),
                            searchErrorMessage = ERROR_GEOCODER_UNAVAILABLE,
                        )
                    }
                }
                is LocationSearchOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            searchStatus = LocationSearchStatus.Error,
                            searchResults = emptyList(),
                            searchErrorMessage = ERROR_SEARCH_FAILED,
                        )
                    }
                }
            }
        }
    }

    fun selectSearchResult(result: com.aaspas.customer.core.location.LocationSearchResult) {
        selectedLocationStore.setManual(
            displayName = result.displayName,
            latitude = result.latitude,
            longitude = result.longitude,
        )
    }

    fun beginCurrentLocationRequest() {
        _uiState.update {
            it.copy(
                isApplyingCurrentLocation = true,
                currentLocationError = null,
            )
        }
    }

    fun onCurrentLocationResolved(displayName: String?, latitude: Double, longitude: Double) {
        selectedLocationStore.setCurrentGps(displayName, latitude, longitude)
        _uiState.update {
            it.copy(
                isApplyingCurrentLocation = false,
                currentLocationError = null,
            )
        }
    }

    fun onCurrentLocationDenied() {
        _uiState.update {
            it.copy(
                isApplyingCurrentLocation = false,
                currentLocationError = ERROR_PERMISSION_DENIED,
            )
        }
    }

    fun onCurrentLocationUnavailable() {
        _uiState.update {
            it.copy(
                isApplyingCurrentLocation = false,
                currentLocationError = ERROR_GPS_UNAVAILABLE,
            )
        }
    }

    fun isCurrentSelection(result: com.aaspas.customer.core.location.LocationSearchResult): Boolean {
        val current = selectedLocationStore.current()
        return current.source == LocationSource.MANUAL &&
            current.displayName == result.displayName &&
            current.latitude == result.latitude &&
            current.longitude == result.longitude
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 350L
        const val ERROR_GEOCODER_UNAVAILABLE = "geocoder_unavailable"
        const val ERROR_SEARCH_FAILED = "search_failed"
        const val ERROR_PERMISSION_DENIED = "permission_denied"
        const val ERROR_GPS_UNAVAILABLE = "gps_unavailable"
    }
}
