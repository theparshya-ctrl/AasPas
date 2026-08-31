package com.aaspas.customer.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.core.location.GeoCoordinates
import com.aaspas.customer.core.location.LocationAccessState
import com.aaspas.customer.domain.map.MapCategoryFilter
import com.aaspas.customer.domain.map.MapMarkerGrouper
import com.aaspas.customer.domain.model.MapShopPin
import com.aaspas.customer.domain.repository.HomeRepository
import com.aaspas.customer.presentation.map.provider.MapProviderSelector
import com.aaspas.customer.presentation.map.provider.MapProviderType
import com.aaspas.customer.presentation.map.provider.MapRenderState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(
    private val homeRepository: HomeRepository,
    googleMapsKeyConfigured: Boolean,
    categoryId: String? = null,
    categoryName: String? = null,
) : ViewModel() {

    private val categoryFilter = MapCategoryFilter(
        categoryId = categoryId?.trim()?.takeIf { it.isNotEmpty() },
        categoryName = categoryName?.trim()?.takeIf { it.isNotEmpty() },
    )

    private val _uiState = MutableStateFlow(
        MapUiState(
            categoryId = categoryFilter.categoryId,
            categoryName = categoryFilter.categoryName,
            googleMapsKeyConfigured = googleMapsKeyConfigured,
            activeProvider = MapProviderSelector.resolve(
                googleMapsKeyConfigured = googleMapsKeyConfigured,
                googleMapsInitFailed = false,
            ),
            mapRenderState = if (googleMapsKeyConfigured) {
                MapRenderState.Loading
            } else {
                MapRenderState.Ready
            },
            viewMode = MapViewMode.Map,
        ),
    )
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null

    fun onPermissionDenied() {
        _uiState.update {
            it.copy(
                locationState = LocationAccessState.Denied,
                userLocation = null,
                needsLocationSelection = lastLatitude == null && lastLongitude == null,
            )
        }
        loadWithoutLocation()
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(locationState = LocationAccessState.Granted) }
    }

    fun setUserLocation(coordinates: GeoCoordinates?, localityName: String? = null) {
        if (coordinates == null) {
            _uiState.update {
                it.copy(
                    locationState = LocationAccessState.Unavailable,
                    userLocation = null,
                    localityName = null,
                    needsLocationSelection = true,
                )
            }
            loadWithoutLocation()
            return
        }
        lastLatitude = coordinates.latitude
        lastLongitude = coordinates.longitude
        _uiState.update {
            it.copy(
                locationState = LocationAccessState.Available,
                userLocation = coordinates,
                localityName = localityName,
                needsLocationSelection = false,
            )
        }
        loadNearby(coordinates.latitude, coordinates.longitude)
    }

    fun beginLoadingLocation() {
        _uiState.update { it.copy(locationState = LocationAccessState.Loading) }
    }

    fun setViewMode(mode: MapViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun selectPin(pin: MapShopPin?) {
        _uiState.update { it.copy(selectedPin = pin) }
    }

    fun selectShopMarker(shopId: String) {
        val pin = _uiState.value.pins.find { it.shop.id == shopId }
        selectPin(pin)
    }

    fun onGoogleMapsReady() {
        if (_uiState.value.activeProvider != MapProviderType.Google) return
        _uiState.update { it.copy(mapRenderState = MapRenderState.Ready) }
    }

    fun onGoogleMapsFailed() {
        if (!_uiState.value.googleMapsKeyConfigured || _uiState.value.googleMapsInitFailed) return
        _uiState.update {
            it.copy(
                googleMapsInitFailed = true,
                activeProvider = MapProviderType.OpenStreetMap,
                mapRenderState = MapRenderState.Loading,
            )
        }
    }

    fun onOpenStreetMapReady() {
        if (_uiState.value.activeProvider != MapProviderType.OpenStreetMap) return
        _uiState.update { it.copy(mapRenderState = MapRenderState.Ready) }
    }

    fun onOpenStreetMapFailed() {
        if (_uiState.value.activeProvider != MapProviderType.OpenStreetMap) return
        _uiState.update { it.copy(mapRenderState = MapRenderState.Unavailable) }
    }

    fun retryMapProvider() {
        _uiState.update {
            it.copy(
                googleMapsInitFailed = false,
                activeProvider = MapProviderSelector.resolve(
                    googleMapsKeyConfigured = it.googleMapsKeyConfigured,
                    googleMapsInitFailed = false,
                ),
                mapRenderState = MapRenderState.Loading,
            )
        }
    }

    fun retry() {
        val lat = lastLatitude
        val lng = lastLongitude
        if (lat != null && lng != null) {
            loadNearby(lat, lng)
        } else {
            loadWithoutLocation()
        }
    }

    private fun loadWithoutLocation() {
        loadNearby(null, null)
    }

    private fun loadNearby(latitude: Double?, longitude: Double?) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = MapLoadStatus.Loading, error = null) }
            when (val result = homeRepository.getHome(latitude, longitude)) {
                is Result.Success -> {
                    val resolvedCategoryName = MapMarkerGrouper.resolveCategoryName(
                        feed = result.data,
                        categoryFilter = categoryFilter,
                    ) ?: categoryFilter.categoryName
                    val pins = MapMarkerGrouper.groupFromHome(
                        feed = result.data,
                        categoryFilter = categoryFilter,
                    )
                    val offerCount = pins.sumOf { pin -> pin.offers.size }
                    _uiState.update {
                        it.copy(
                            status = MapLoadStatus.Loaded,
                            pins = pins,
                            selectedPin = null,
                            categoryName = resolvedCategoryName ?: it.categoryName,
                            categoryOfferCount = offerCount,
                            error = null,
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            status = MapLoadStatus.Error,
                            pins = emptyList(),
                            categoryOfferCount = 0,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }
}
