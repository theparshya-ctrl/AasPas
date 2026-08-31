package com.aaspas.customer.presentation.map.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aaspas.customer.domain.model.MapShopPin
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay

private const val GOOGLE_MAP_LOAD_TIMEOUT_MS = 8_000L

@Composable
fun GoogleMapProviderView(
    pins: List<MapShopPin>,
    centerLatitude: Double,
    centerLongitude: Double,
    zoomLevel: Double,
    selectedShopId: String?,
    showMyLocation: Boolean,
    onShopMarkerSelected: (String) -> Unit,
    onMapReady: () -> Unit,
    onMapFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mapCenter = LatLng(centerLatitude, centerLongitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(mapCenter, zoomLevel.toFloat())
    }
    val markerStates = remember(pins) {
        pins.mapNotNull { pin ->
            val lat = pin.shop.latitude ?: return@mapNotNull null
            val lng = pin.shop.longitude ?: return@mapNotNull null
            pin.shop.id to MarkerState(position = LatLng(lat, lng))
        }.toMap()
    }
    var mapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(centerLatitude, centerLongitude, zoomLevel) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(mapCenter, zoomLevel.toFloat())
    }

    LaunchedEffect(Unit) {
        delay(GOOGLE_MAP_LOAD_TIMEOUT_MS)
        if (!mapLoaded) {
            onMapFailed()
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = showMyLocation),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = showMyLocation,
        ),
        onMapLoaded = {
            mapLoaded = true
            onMapReady()
        },
    ) {
        pins.forEach { pin ->
            val markerState = markerStates[pin.shop.id] ?: return@forEach
            val selected = selectedShopId == pin.shop.id
            Marker(
                state = markerState,
                title = pin.shop.name,
                snippet = pin.featuredOffer?.title,
                onClick = {
                    onShopMarkerSelected(pin.shop.id)
                    true
                },
                alpha = if (selected) 1f else 0.9f,
            )
        }
    }
}
