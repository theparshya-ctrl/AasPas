package com.aaspas.customer.presentation.map

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.location.GeoCoordinates
import com.aaspas.customer.core.location.LocationAccessState
import com.aaspas.customer.domain.model.MapShopPin
import com.aaspas.customer.presentation.map.provider.MapProviderType
import com.aaspas.customer.presentation.map.provider.MapRenderState

enum class MapViewMode {
    Map,
    List,
}

enum class MapLoadStatus {
    Loading,
    Loaded,
    Empty,
    Error,
}

data class MapUiState(
    val locationState: LocationAccessState = LocationAccessState.NotRequested,
    val userLocation: GeoCoordinates? = null,
    val localityName: String? = null,
    val status: MapLoadStatus = MapLoadStatus.Loading,
    val viewMode: MapViewMode = MapViewMode.Map,
    val pins: List<MapShopPin> = emptyList(),
    val selectedPin: MapShopPin? = null,
    val error: AppError? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val categoryOfferCount: Int = 0,
    val googleMapsKeyConfigured: Boolean = false,
    val googleMapsInitFailed: Boolean = false,
    val activeProvider: MapProviderType = MapProviderType.OpenStreetMap,
    val mapRenderState: MapRenderState = MapRenderState.Loading,
    val needsLocationSelection: Boolean = false,
)
