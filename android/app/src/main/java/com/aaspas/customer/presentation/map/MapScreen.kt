package com.aaspas.customer.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaspas.customer.AasPasApplication
import com.aaspas.customer.BuildConfig
import com.aaspas.customer.R
import com.aaspas.customer.core.location.AndroidLocationProvider
import com.aaspas.customer.core.location.LocalityResolver
import com.aaspas.customer.core.location.LocationAccessState
import com.aaspas.customer.core.navigation.DirectionsIntentBuilder
import com.aaspas.customer.domain.model.LocationSource
import com.aaspas.customer.domain.model.MapShopPin
import com.aaspas.customer.presentation.components.CategoryMapEmptyState
import com.aaspas.customer.presentation.components.EmptyState
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.components.OfferCard
import com.aaspas.customer.presentation.location.LocationPermissionHandler
import com.aaspas.customer.presentation.map.provider.GoogleMapProviderView
import com.aaspas.customer.presentation.map.provider.MapDefaults
import com.aaspas.customer.presentation.map.provider.MapProviderType
import com.aaspas.customer.presentation.map.provider.MapRenderState
import com.aaspas.customer.presentation.map.provider.OpenStreetMapProviderView
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasRadius
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun MapRoute(
    categoryId: String? = null,
    categoryName: String? = null,
    onBack: () -> Unit,
    onShopClick: (String) -> Unit,
    onOfferClick: (String) -> Unit,
    onChangeLocation: () -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val googleMapsKeyConfigured = BuildConfig.MAPS_API_KEY.isNotBlank()
    val viewModel: MapViewModel = viewModel(
        factory = MapViewModelFactory(app.homeRepository, googleMapsKeyConfigured, categoryId, categoryName),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLocation by app.selectedLocationStore.selectedLocation.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationProvider = remember { AndroidLocationProvider(context) }
    val localityResolver = remember { LocalityResolver(context) }
    val useStoredLocation = selectedLocation.hasCoordinates &&
        (selectedLocation.source == LocationSource.MANUAL || selectedLocation.source == LocationSource.CURRENT_GPS)

    LaunchedEffect(selectedLocation) {
        if (selectedLocation.hasCoordinates) {
            viewModel.setUserLocation(selectedLocation.coordinates, selectedLocation.displayName)
        }
    }

    LocationPermissionHandler(
        locationProvider = locationProvider,
        localityResolver = localityResolver,
        requestOnLaunch = !useStoredLocation,
        onDenied = viewModel::onPermissionDenied,
        onGranted = {
            if (selectedLocation.source == LocationSource.MANUAL && selectedLocation.hasCoordinates) {
                return@LocationPermissionHandler
            }
            viewModel.onPermissionGranted()
            viewModel.setUserLocation(it.coordinates, it.localityName)
        },
        onUnavailable = {
            if (!selectedLocation.hasCoordinates) {
                viewModel.setUserLocation(null)
            }
        },
        onLoading = viewModel::beginLoadingLocation,
    )

    MapScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRetryMap = viewModel::retryMapProvider,
        onViewModeChange = viewModel::setViewMode,
        onShopMarkerSelected = viewModel::selectShopMarker,
        onDismissPin = { viewModel.selectPin(null) },
        onGoogleMapsReady = viewModel::onGoogleMapsReady,
        onGoogleMapsFailed = viewModel::onGoogleMapsFailed,
        onOpenStreetMapReady = viewModel::onOpenStreetMapReady,
        onOpenStreetMapFailed = viewModel::onOpenStreetMapFailed,
        onChangeLocation = onChangeLocation,
        onShopClick = onShopClick,
        onOfferClick = onOfferClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    uiState: MapUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRetryMap: () -> Unit,
    onViewModeChange: (MapViewMode) -> Unit,
    onShopMarkerSelected: (String) -> Unit,
    onDismissPin: () -> Unit,
    onGoogleMapsReady: () -> Unit,
    onGoogleMapsFailed: () -> Unit,
    onOpenStreetMapReady: () -> Unit,
    onOpenStreetMapFailed: () -> Unit,
    onChangeLocation: () -> Unit,
    onShopClick: (String) -> Unit,
    onOfferClick: (String) -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.map_title))
                        uiState.categoryName?.let { category ->
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelMedium,
                                color = AasPasColors.TextSecondary,
                            )
                            if (uiState.categoryOfferCount > 0) {
                                Text(
                                    text = stringResource(
                                        R.string.map_category_offer_count,
                                        uiState.categoryOfferCount,
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AasPasColors.TextSecondary,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        containerColor = AasPasColors.Background,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            MapLocationBanner(uiState = uiState)
            if (uiState.needsLocationSelection) {
                MapLocationPrompt(onChangeLocation = onChangeLocation)
            }
            MapModeToggle(
                viewMode = uiState.viewMode,
                onViewModeChange = onViewModeChange,
            )
            when (uiState.status) {
                MapLoadStatus.Loading -> MapLoading(modifier = Modifier.weight(1f))
                MapLoadStatus.Error -> ErrorState(
                    error = uiState.error,
                    onRetry = onRetry,
                    fallbackMessageResId = R.string.error_map,
                    modifier = Modifier
                        .weight(1f)
                        .padding(AasPasSpacing.lg),
                )
                MapLoadStatus.Loaded, MapLoadStatus.Empty -> {
                    if (uiState.viewMode == MapViewMode.Map) {
                        MapDiscoveryView(
                            modifier = Modifier.weight(1f),
                            uiState = uiState,
                            onShopMarkerSelected = onShopMarkerSelected,
                            onDismissPin = onDismissPin,
                            onViewShop = { onShopClick(it) },
                            onViewOffer = { onOfferClick(it) },
                            onDirections = { pin -> openDirections(context, pin) },
                            onGoogleMapsReady = onGoogleMapsReady,
                            onGoogleMapsFailed = onGoogleMapsFailed,
                            onOpenStreetMapReady = onOpenStreetMapReady,
                            onOpenStreetMapFailed = onOpenStreetMapFailed,
                            onRetryMap = onRetryMap,
                            onViewList = { onViewModeChange(MapViewMode.List) },
                            onChangeLocation = onChangeLocation,
                        )
                    } else {
                        MapListView(
                            modifier = Modifier.weight(1f),
                            uiState = uiState,
                            onOfferClick = onOfferClick,
                            onShopClick = onShopClick,
                            onChangeLocation = onChangeLocation,
                        )
                    }
                }
            }
        }
    }
}

private fun openDirections(context: android.content.Context, pin: MapShopPin) {
    val intent = DirectionsIntentBuilder.build(
        latitude = pin.shop.latitude,
        longitude = pin.shop.longitude,
        label = pin.shop.name,
    ) ?: DirectionsIntentBuilder.buildFallback(
        latitude = pin.shop.latitude,
        longitude = pin.shop.longitude,
        label = pin.shop.name,
    )
    intent?.let {
        if (it.resolveActivity(context.packageManager) != null) {
            context.startActivity(it)
        }
    }
}

@Composable
private fun MapLocationBanner(uiState: MapUiState) {
    val text = when (uiState.locationState) {
        LocationAccessState.Loading -> stringResource(R.string.home_location_loading)
        LocationAccessState.Denied -> stringResource(R.string.map_location_denied_hint)
        LocationAccessState.Unavailable -> stringResource(R.string.home_location_unavailable)
        LocationAccessState.Available -> uiState.localityName ?: stringResource(R.string.home_location_near_you)
        LocationAccessState.Granted -> stringResource(R.string.home_location_loading)
        LocationAccessState.NotRequested -> stringResource(R.string.home_location_loading)
    }
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = AasPasSpacing.lg, vertical = AasPasSpacing.sm),
        style = MaterialTheme.typography.bodyMedium,
        color = AasPasColors.TextSecondary,
    )
}

@Composable
private fun MapLocationPrompt(onChangeLocation: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AasPasSpacing.lg, vertical = AasPasSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.map_location_prompt),
            style = MaterialTheme.typography.bodyMedium,
            color = AasPasColors.TextSecondary,
        )
        OutlinedButton(onClick = onChangeLocation) {
            Text(stringResource(R.string.location_change_action))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapModeToggle(
    viewMode: MapViewMode,
    onViewModeChange: (MapViewMode) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AasPasSpacing.lg, vertical = AasPasSpacing.xs),
    ) {
        SegmentedButton(
            selected = viewMode == MapViewMode.Map,
            onClick = { onViewModeChange(MapViewMode.Map) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) { Text(stringResource(R.string.map_view_map)) }
        SegmentedButton(
            selected = viewMode == MapViewMode.List,
            onClick = { onViewModeChange(MapViewMode.List) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) { Text(stringResource(R.string.map_view_list)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapDiscoveryView(
    uiState: MapUiState,
    onShopMarkerSelected: (String) -> Unit,
    onDismissPin: () -> Unit,
    onViewShop: (String) -> Unit,
    onViewOffer: (String) -> Unit,
    onDirections: (MapShopPin) -> Unit,
    onGoogleMapsReady: () -> Unit,
    onGoogleMapsFailed: () -> Unit,
    onOpenStreetMapReady: () -> Unit,
    onOpenStreetMapFailed: () -> Unit,
    onRetryMap: () -> Unit,
    onViewList: () -> Unit,
    onChangeLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val center = uiState.userLocation ?: MapDefaults.PILOT_FALLBACK_CENTER
    val zoomLevel = MapDefaults.LOCAL_DISCOVERY_ZOOM
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedPin = uiState.selectedPin
    val showMyLocation = uiState.locationState == LocationAccessState.Available

    LaunchedEffect(selectedPin) {
        if (selectedPin != null) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (uiState.mapRenderState) {
            MapRenderState.Unavailable -> {
                MapUnavailableState(
                    onRetryMap = onRetryMap,
                    onViewList = onViewList,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                when (uiState.activeProvider) {
                    MapProviderType.Google -> {
                        GoogleMapProviderView(
                            pins = uiState.pins,
                            centerLatitude = center.latitude,
                            centerLongitude = center.longitude,
                            zoomLevel = zoomLevel,
                            selectedShopId = selectedPin?.shop?.id,
                            showMyLocation = showMyLocation,
                            onShopMarkerSelected = onShopMarkerSelected,
                            onMapReady = onGoogleMapsReady,
                            onMapFailed = onGoogleMapsFailed,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    MapProviderType.OpenStreetMap -> {
                        OpenStreetMapProviderView(
                            pins = uiState.pins,
                            centerLatitude = center.latitude,
                            centerLongitude = center.longitude,
                            zoomLevel = zoomLevel,
                            selectedShopId = selectedPin?.shop?.id,
                            onShopMarkerSelected = onShopMarkerSelected,
                            onMapReady = onOpenStreetMapReady,
                            onMapFailed = onOpenStreetMapFailed,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                if (uiState.mapRenderState == MapRenderState.Loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = AasPasColors.PurplePrimary)
                    }
                }
                if (uiState.pins.isEmpty() && uiState.mapRenderState != MapRenderState.Unavailable) {
                    MapEmptyOffersOverlay(
                        isCategoryFilter = uiState.categoryId != null || uiState.categoryName != null,
                        onChangeLocation = onChangeLocation,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(AasPasSpacing.lg),
                    )
                }
            }
        }
    }

    if (selectedPin != null) {
        ModalBottomSheet(
            onDismissRequest = onDismissPin,
            sheetState = sheetState,
        ) {
            MapMarkerSheetContent(
                pin = selectedPin,
                onViewShop = { onViewShop(selectedPin.shop.id) },
                onViewOffer = onViewOffer,
                onDirections = { onDirections(selectedPin) },
                modifier = Modifier.padding(bottom = AasPasSpacing.lg),
            )
        }
    }
}

@Composable
private fun MapUnavailableState(
    onRetryMap: () -> Unit,
    onViewList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(AasPasSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.map_unavailable_title),
            style = MaterialTheme.typography.titleMedium,
            color = AasPasColors.TextPrimary,
        )
        Text(
            text = stringResource(R.string.map_unavailable_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = AasPasColors.TextSecondary,
        )
        OutlinedButton(onClick = onRetryMap) {
            Text(stringResource(R.string.retry))
        }
        Button(onClick = onViewList) {
            Text(stringResource(R.string.map_view_list))
        }
    }
}

@Composable
private fun MapListView(
    uiState: MapUiState,
    onOfferClick: (String) -> Unit,
    onShopClick: (String) -> Unit,
    onChangeLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.pins.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (uiState.categoryId != null || uiState.categoryName != null) {
                CategoryMapEmptyState(
                    onChangeLocation = onChangeLocation,
                    modifier = Modifier.padding(AasPasSpacing.lg),
                )
            } else {
                EmptyState(
                    title = stringResource(R.string.map_empty_title),
                    hint = stringResource(R.string.map_empty_hint),
                    modifier = Modifier.padding(AasPasSpacing.lg),
                )
            }
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(AasPasSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
    ) {
        items(uiState.pins, key = { it.shop.id }) { pin ->
            pin.featuredOffer?.let { offer ->
                OfferCard(
                    offer = offer,
                    onClick = { onOfferClick(offer.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } ?: Button(onClick = { onShopClick(pin.shop.id) }) {
                Text(pin.shop.name)
            }
        }
    }
}

@Composable
private fun MapEmptyOffersOverlay(
    isCategoryFilter: Boolean,
    onChangeLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AasPasColors.Background.copy(alpha = 0.94f),
                shape = RoundedCornerShape(AasPasRadius.md),
            )
            .padding(AasPasSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isCategoryFilter) {
            CategoryMapEmptyState(onChangeLocation = onChangeLocation)
        } else {
            EmptyState(
                title = stringResource(R.string.map_empty_title),
                hint = stringResource(R.string.map_empty_hint),
            )
        }
    }
}

@Composable
private fun MapLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AasPasColors.PurplePrimary)
    }
}
