package com.aaspas.customer.presentation.map.provider

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aaspas.customer.R
import com.aaspas.customer.domain.model.MapShopPin
import com.aaspas.customer.presentation.theme.AasPasSpacing
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker

private val ShopMarkerTag = Any()

/**
 * Native OpenStreetMap tiles via osmdroid (Apache 2.0).
 * Used when Google Maps key is missing or Google Maps init fails.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun OpenStreetMapProviderView(
    pins: List<MapShopPin>,
    centerLatitude: Double,
    centerLongitude: Double,
    zoomLevel: Double,
    selectedShopId: String?,
    onShopMarkerSelected: (String) -> Unit,
    onMapReady: () -> Unit,
    onMapFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapViewHolder = remember { mutableStateOf<MapView?>(null) }
    val readySent = remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val mapView = mapViewHolder.value ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewHolder.value?.onDetach()
            mapViewHolder.value = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    isTilesScaledToDpi = true
                    controller.setZoom(zoomLevel)
                    controller.setCenter(GeoPoint(centerLatitude, centerLongitude))
                    if (overlays.none { it is CopyrightOverlay }) {
                        overlays.add(CopyrightOverlay(context))
                    }
                    syncShopMarkers(
                        pins = pins,
                        selectedShopId = selectedShopId,
                        onShopMarkerSelected = onShopMarkerSelected,
                    )
                    mapViewHolder.value = this
                    if (!readySent.value) {
                        readySent.value = true
                        onMapReady()
                    }
                }
            },
            update = { mapView ->
                mapView.controller.setCenter(GeoPoint(centerLatitude, centerLongitude))
                mapView.controller.setZoom(zoomLevel)
                mapView.syncShopMarkers(
                    pins = pins,
                    selectedShopId = selectedShopId,
                    onShopMarkerSelected = onShopMarkerSelected,
                )
                mapView.invalidate()
            },
        )
        Text(
            text = stringResource(R.string.map_osm_attribution),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = AasPasSpacing.sm, bottom = AasPasSpacing.sm),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

private fun MapView.syncShopMarkers(
    pins: List<MapShopPin>,
    selectedShopId: String?,
    onShopMarkerSelected: (String) -> Unit,
) {
    overlays.removeAll { overlay -> overlay is Marker && overlay.relatedObject === ShopMarkerTag }
    pins.forEach { pin ->
        val lat = pin.shop.latitude ?: return@forEach
        val lng = pin.shop.longitude ?: return@forEach
        val marker = Marker(this).apply {
            position = GeoPoint(lat, lng)
            title = pin.shop.name
            snippet = pin.featuredOffer?.title
            relatedObject = ShopMarkerTag
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            alpha = if (pin.shop.id == selectedShopId) 1f else 0.92f
            setOnMarkerClickListener { _, _ ->
                onShopMarkerSelected(pin.shop.id)
                true
            }
        }
        overlays.add(marker)
    }
}
