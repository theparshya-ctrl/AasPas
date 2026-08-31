package com.aaspas.customer.presentation.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaspas.customer.AasPasApplication
import com.aaspas.customer.R
import com.aaspas.customer.core.location.AndroidLocationProvider
import com.aaspas.customer.core.location.LocalityResolver
import com.aaspas.customer.core.location.LocationSearchResult
import com.aaspas.customer.domain.model.LocationSource
import com.aaspas.customer.presentation.components.EmptyState
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing
import kotlinx.coroutines.launch

@Composable
fun LocationSelectionRoute(
    onBack: () -> Unit,
    onLocationApplied: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: LocationSelectionViewModel = viewModel(
        factory = LocationSelectionViewModelFactory(
            app.locationSearchService,
            app.selectedLocationStore,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationProvider = remember { AndroidLocationProvider(context) }
    val localityResolver = remember { LocalityResolver(context) }
    var requestCurrentLocation by remember { mutableStateOf(false) }

    if (requestCurrentLocation) {
        RequestLocationPermission(
            locationProvider = locationProvider,
            localityResolver = localityResolver,
            onLoading = viewModel::beginCurrentLocationRequest,
            onDenied = {
                requestCurrentLocation = false
                viewModel.onCurrentLocationDenied()
            },
            onUnavailable = {
                requestCurrentLocation = false
                viewModel.onCurrentLocationUnavailable()
            },
            onGranted = { resolved ->
                requestCurrentLocation = false
                viewModel.onCurrentLocationResolved(
                    displayName = resolved.localityName,
                    latitude = resolved.coordinates.latitude,
                    longitude = resolved.coordinates.longitude,
                )
                onLocationApplied()
            },
        )
    }

    LocationSelectionScreen(
        uiState = uiState,
        onBack = onBack,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onUseCurrentLocation = { requestCurrentLocation = true },
        onResultSelected = { result ->
            viewModel.selectSearchResult(result)
            onLocationApplied()
        },
        isResultSelected = viewModel::isCurrentSelection,
        resolveErrorMessage = { key ->
            when (key) {
                LocationSelectionViewModel.ERROR_GEOCODER_UNAVAILABLE ->
                    context.getString(R.string.location_search_geocoder_unavailable)
                LocationSelectionViewModel.ERROR_SEARCH_FAILED ->
                    context.getString(R.string.location_search_error)
                LocationSelectionViewModel.ERROR_PERMISSION_DENIED ->
                    context.getString(R.string.location_search_permission_denied)
                LocationSelectionViewModel.ERROR_GPS_UNAVAILABLE ->
                    context.getString(R.string.location_search_gps_unavailable)
                else -> key
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionScreen(
    uiState: LocationSelectionUiState,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onResultSelected: (LocationSearchResult) -> Unit,
    isResultSelected: (LocationSearchResult) -> Boolean,
    resolveErrorMessage: (String) -> String,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.location_selection_title)) },
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(AasPasSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
        ) {
            item {
                OutlinedButton(
                    onClick = onUseCurrentLocation,
                    enabled = !uiState.isApplyingCurrentLocation,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.isApplyingCurrentLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = AasPasSpacing.sm),
                            color = AasPasColors.PurplePrimary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = null,
                            tint = AasPasColors.PurplePrimary,
                            modifier = Modifier.padding(end = AasPasSpacing.sm),
                        )
                    }
                    Text(stringResource(R.string.use_current_location))
                }
                uiState.currentLocationError?.let { key ->
                    Text(
                        text = resolveErrorMessage(key),
                        color = AasPasColors.RedCritical,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = AasPasSpacing.xs),
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text(stringResource(R.string.location_search_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            when (uiState.searchStatus) {
                LocationSearchStatus.Loading -> {
                    item {
                        CircularProgressIndicator(color = AasPasColors.PurplePrimary)
                    }
                }
                LocationSearchStatus.Empty -> {
                    item {
                        EmptyState(
                            title = stringResource(R.string.location_search_no_results_title),
                            hint = stringResource(R.string.location_search_no_results_hint),
                        )
                    }
                }
                LocationSearchStatus.Error -> {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
                        ) {
                            Text(
                                text = uiState.searchErrorMessage?.let(resolveErrorMessage)
                                    ?: stringResource(R.string.location_search_error),
                                color = AasPasColors.RedCritical,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedButton(onClick = { onSearchQueryChange(uiState.searchQuery) }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
                LocationSearchStatus.Loaded -> {
                    items(uiState.searchResults, key = { "${it.displayName}|${it.latitude}|${it.longitude}" }) { result ->
                        LocationSearchResultRow(
                            result = result,
                            selected = isResultSelected(result),
                            onClick = { onResultSelected(result) },
                        )
                    }
                }
                LocationSearchStatus.Idle -> {
                    if (uiState.currentSelection.source != LocationSource.NONE &&
                        uiState.currentSelection.hasCoordinates
                    ) {
                        item {
                            CurrentSelectionCard(
                                displayName = uiState.currentSelection.displayName
                                    ?: stringResource(R.string.home_location_near_you),
                                source = uiState.currentSelection.source,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationSearchResultRow(
    result: LocationSearchResult,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(result.displayName) },
        supportingContent = {
            Text(
                text = stringResource(
                    R.string.location_search_coordinates_hint,
                    result.latitude,
                    result.longitude,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AasPasColors.TextSecondary,
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = if (selected) AasPasColors.PurplePrimary else AasPasColors.TextSecondary,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) AasPasColors.PurplePrimary.copy(alpha = 0.08f) else AasPasColors.Background,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
private fun CurrentSelectionCard(
    displayName: String,
    source: LocationSource,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.location_current_selection_label),
            style = MaterialTheme.typography.labelMedium,
            color = AasPasColors.TextSecondary,
        )
        Text(
            text = displayName,
            style = MaterialTheme.typography.titleMedium,
            color = AasPasColors.PurplePrimary,
        )
        Text(
            text = when (source) {
                LocationSource.MANUAL -> stringResource(R.string.location_selected_subtitle)
                LocationSource.CURRENT_GPS -> stringResource(R.string.location_current_subtitle)
                LocationSource.NONE -> stringResource(R.string.location_choose_subtitle)
            },
            style = MaterialTheme.typography.bodySmall,
            color = AasPasColors.TextSecondary,
        )
    }
}
