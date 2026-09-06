package com.aaspas.customer.presentation.home



import android.Manifest

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.LazyRow

import androidx.compose.foundation.lazy.items

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.SnackbarHost

import androidx.compose.material3.SnackbarHostState

import androidx.compose.material3.pulltorefresh.PullToRefreshBox

import androidx.compose.runtime.Composable

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.res.stringResource

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.lifecycle.viewmodel.compose.viewModel

import com.aaspas.customer.AasPasApplication

import com.aaspas.customer.R

import com.aaspas.customer.core.common.AppError

import com.aaspas.customer.core.location.AndroidLocationProvider

import com.aaspas.customer.core.location.LocalityResolver
import com.aaspas.customer.core.location.LocationFreshness

import com.aaspas.customer.domain.model.Category

import com.aaspas.customer.domain.model.LocationSource

import com.aaspas.customer.domain.model.Offer

import com.aaspas.customer.domain.model.SelectedLocation

import com.aaspas.customer.domain.model.Shop

import com.aaspas.customer.core.auth.SessionState

import com.aaspas.customer.presentation.components.EmptyState

import com.aaspas.customer.presentation.components.ErrorState

import com.aaspas.customer.presentation.components.GlobalErrorBanner

import com.aaspas.customer.presentation.components.NotificationBellButton
import com.aaspas.customer.presentation.components.LocationHeader

import com.aaspas.customer.presentation.components.MapCtaCard

import com.aaspas.customer.presentation.components.OfferCard

import com.aaspas.customer.presentation.components.OfferCardsSkeleton

import com.aaspas.customer.presentation.components.SearchBar

import com.aaspas.customer.presentation.components.SectionHeader

import com.aaspas.customer.presentation.components.ShopCard

import com.aaspas.customer.presentation.components.ShopCardsSkeleton

import com.aaspas.customer.presentation.components.CategorySection

import com.aaspas.customer.presentation.theme.AasPasSpacing

import kotlinx.coroutines.launch



@Composable

fun HomeRoute(

    onOfferClick: (String) -> Unit,

    onShopClick: (String) -> Unit,

    onSearchClick: () -> Unit,

    onMapClick: () -> Unit,

    onCategoryClick: (Category) -> Unit,

    onNavigateToLogin: (() -> Unit) -> Unit,

    onChangeLocationClick: () -> Unit,

    onNotificationsClick: () -> Unit = {},

) {

    val app = LocalContext.current.applicationContext as AasPasApplication

    val viewModel: HomeViewModel = viewModel(

        factory = HomeViewModelFactory(

            app.homeRepository,

            app.favoritesRepository,

            app.authRepository,

            app.notificationRepository,

            app.realtimeNotificationStore,

        ),

    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val selectedLocation by app.selectedLocationStore.selectedLocation.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val locationProvider = remember { AndroidLocationProvider(context) }

    val localityResolver = remember { LocalityResolver(context) }

    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    val sessionState by app.authRepository.sessionState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(sessionState) {
        viewModel.onLoginStateChanged(sessionState == SessionState.LoggedIn)
    }

    DisposableEffect(lifecycleOwner, sessionState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && sessionState == SessionState.LoggedIn) {
                viewModel.refreshNotifications()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var initialLocationHandled by remember { mutableStateOf(false) }

    var previousLocation by remember { mutableStateOf<SelectedLocation?>(null) }



    fun handleSaveError(error: AppError) {

        scope.launch {

            val message = when (error) {

                AppError.NoInternet -> context.getString(R.string.error_network)

                AppError.Timeout -> context.getString(R.string.error_timeout)

                else -> context.getString(R.string.error_server)

            }

            snackbarHostState.showSnackbar(message)

        }

    }



    fun handleOfferSave(offerId: String) {

        viewModel.toggleOfferSave(offerId) { outcome ->

            when (outcome) {

                HomeViewModel.SaveToggleOutcome.NeedLogin -> {

                    onNavigateToLogin { viewModel.toggleOfferSave(offerId) {} }

                }

                is HomeViewModel.SaveToggleOutcome.Error -> handleSaveError(outcome.error)

                HomeViewModel.SaveToggleOutcome.Success -> Unit

            }

        }

    }



    fun handleShopSave(shopId: String) {

        viewModel.toggleShopSave(shopId) { outcome ->

            when (outcome) {

                HomeViewModel.SaveToggleOutcome.NeedLogin -> {

                    onNavigateToLogin { viewModel.toggleShopSave(shopId) {} }

                }

                is HomeViewModel.SaveToggleOutcome.Error -> handleSaveError(outcome.error)

                HomeViewModel.SaveToggleOutcome.Success -> Unit

            }

        }

    }



    fun loadFromSelectedLocation(
        location: SelectedLocation,
        isRefresh: Boolean,
        forceReload: Boolean = false,
    ) {
        viewModel.applySelectedLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            displayName = location.displayName,
            source = location.source,
            locationDenied = false,
            isRefresh = isRefresh,
            forceReload = forceReload,
        )
    }

    suspend fun refreshGpsInBackground() {
        if (!locationProvider.hasLocationPermission()) return
        val coords = locationProvider.getLastLocation() ?: return
        val current = app.selectedLocationStore.current()
        val locality = localityResolver.resolveLocality(coords)
        if (LocationFreshness.isMaterialChange(current, coords.latitude, coords.longitude)) {
            app.selectedLocationStore.setCurrentGps(locality, coords.latitude, coords.longitude)
            return
        }
        if (!locality.isNullOrBlank() && locality != current.displayName) {
            viewModel.setLocalityName(locality, LocationSource.CURRENT_GPS)
            app.selectedLocationStore.updateDisplayNameOnly(locality)
        }
    }

    suspend fun loadFromGps(isRefresh: Boolean, forceReload: Boolean = isRefresh) {

        val coords = locationProvider.getLastLocation()

        if (coords == null) {

            viewModel.applySelectedLocation(

                latitude = null,

                longitude = null,

                displayName = null,

                source = LocationSource.NONE,

                locationDenied = false,

                isRefresh = isRefresh,

            )

            return

        }

        val locality = localityResolver.resolveLocality(coords)

        app.selectedLocationStore.setCurrentGps(locality, coords.latitude, coords.longitude)

        viewModel.applySelectedLocation(
            latitude = coords.latitude,
            longitude = coords.longitude,
            displayName = locality,
            source = LocationSource.CURRENT_GPS,
            locationDenied = false,
            isRefresh = isRefresh,
            forceReload = true,
        )
    }



    val permissionLauncher = rememberLauncherForActivityResult(

        ActivityResultContracts.RequestMultiplePermissions(),

    ) { results ->

        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||

            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        scope.launch {

            if (granted) {

                loadFromGps(isRefresh = false)

            } else {

                val restored = app.selectedLocationStore.current()

                if (restored.source == LocationSource.MANUAL && restored.hasCoordinates) {

                    loadFromSelectedLocation(restored, isRefresh = false)

                } else {

                    viewModel.applySelectedLocation(

                        latitude = null,

                        longitude = null,

                        displayName = null,

                        source = LocationSource.NONE,

                        locationDenied = true,

                        isRefresh = false,

                    )

                }

            }

        }

    }



    fun requestGpsLocation(isRefresh: Boolean = false) {

        if (locationProvider.hasLocationPermission()) {

            scope.launch { loadFromGps(isRefresh) }

        } else {

            permissionLauncher.launch(

                arrayOf(

                    Manifest.permission.ACCESS_FINE_LOCATION,

                    Manifest.permission.ACCESS_COARSE_LOCATION,

                ),

            )

        }

    }



    LaunchedEffect(sessionState) {

        viewModel.onSessionChanged(sessionState == SessionState.LoggedIn)

    }



    LaunchedEffect(Unit) {

        if (!viewModel.tryBeginInitialLoad()) return@LaunchedEffect

        val restored = app.selectedLocationStore.current()

        if (restored.source == LocationSource.MANUAL && restored.hasCoordinates) {

            loadFromSelectedLocation(restored, isRefresh = false)

            initialLocationHandled = true

            previousLocation = restored

            return@LaunchedEffect

        }

        if (restored.source == LocationSource.CURRENT_GPS && restored.hasCoordinates) {
            initialLocationHandled = true
            previousLocation = restored
            if (app.selectedLocationStore.isLocationFresh()) {
                loadFromSelectedLocation(restored, isRefresh = false)
                scope.launch { refreshGpsInBackground() }
            } else {
                scope.launch { loadFromGps(isRefresh = false) }
            }
            return@LaunchedEffect
        }

        requestGpsLocation(isRefresh = false)

        initialLocationHandled = true

        previousLocation = app.selectedLocationStore.current()

    }



    LaunchedEffect(selectedLocation) {
        if (!initialLocationHandled) return@LaunchedEffect
        val prior = previousLocation
        if (prior == selectedLocation) return@LaunchedEffect

        val materialChange = prior == null ||
            !prior.hasCoordinates ||
            !selectedLocation.hasCoordinates ||
            LocationFreshness.isMaterialChange(prior, selectedLocation)

        previousLocation = selectedLocation

        if (selectedLocation.hasCoordinates && materialChange) {
            loadFromSelectedLocation(
                location = selectedLocation,
                isRefresh = prior?.hasCoordinates == true,
            )
        } else if (
            !selectedLocation.displayName.isNullOrBlank() &&
            selectedLocation.displayName != prior?.displayName
        ) {
            viewModel.setLocalityName(selectedLocation.displayName, selectedLocation.source)
        }
    }



    Box(modifier = Modifier.fillMaxSize()) {

        HomeScreen(

            uiState = uiState,

            onRetry = viewModel::retry,

            onRefresh = {
                val current = app.selectedLocationStore.current()
                if (current.source == LocationSource.MANUAL && current.hasCoordinates) {
                    loadFromSelectedLocation(current, isRefresh = true, forceReload = true)
                } else {
                    scope.launch { loadFromGps(isRefresh = true, forceReload = true) }
                }
            },

            onOfferClick = onOfferClick,

            onShopClick = onShopClick,

            onOfferSaveClick = ::handleOfferSave,

            onShopSaveClick = ::handleShopSave,

            onSearchClick = onSearchClick,

            onMapClick = onMapClick,

            onCategoryClick = onCategoryClick,

            onChangeLocationClick = onChangeLocationClick,

            onNotificationsClick = onNotificationsClick,

        )

        SnackbarHost(

            hostState = snackbarHostState,

            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),

        )

    }

}



@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun HomeScreen(

    uiState: HomeUiState,

    onRetry: () -> Unit,

    onRefresh: () -> Unit,

    onOfferClick: (String) -> Unit,

    onShopClick: (String) -> Unit,

    onOfferSaveClick: (String) -> Unit,

    onShopSaveClick: (String) -> Unit,

    onSearchClick: () -> Unit,

    onMapClick: () -> Unit,

    onCategoryClick: (Category) -> Unit,

    onChangeLocationClick: () -> Unit,

    onNotificationsClick: () -> Unit = {},

) {

    PullToRefreshBox(

        isRefreshing = uiState.isRefreshing,

        onRefresh = onRefresh,

        modifier = Modifier.fillMaxSize(),

    ) {

        LazyColumn(

            modifier = Modifier.fillMaxSize(),

            contentPadding = PaddingValues(bottom = AasPasSpacing.xxl),

            verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),

        ) {

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LocationHeader(
                        locationLabel = uiState.locationLabel,
                        locationSource = uiState.locationSource,
                        locationAvailable = uiState.locationAvailable,
                        locationDenied = uiState.locationDenied,
                        localityName = uiState.localityName,
                        onClick = onChangeLocationClick,
                        modifier = Modifier.weight(1f),
                    )
                    if (uiState.showNotificationBell) {
                        NotificationBellButton(
                            unreadCount = uiState.unreadNotificationCount,
                            onClick = onNotificationsClick,
                        )
                    }
                }

            }

            item {

                SearchBar(onClick = onSearchClick)

            }

            if (uiState.globalError != null || uiState.globalErrorType != null) {

                item {

                    GlobalErrorBanner(

                        error = uiState.globalErrorType,

                        fallbackMessage = uiState.globalError,

                        onRetry = onRetry,

                    )

                }

            }

            item {

                CategorySection(

                    categories = uiState.categories.items,

                    status = uiState.categories.status,

                    error = uiState.categories.errorType,

                    onCategoryClick = onCategoryClick,

                )

            }

            item {

                OfferSection(

                    title = stringResource(R.string.section_today_offers),

                    status = uiState.todayOffers.status,

                    offers = uiState.todayOffers.items,

                    error = uiState.todayOffers.errorType,

                    emptyTitle = stringResource(R.string.empty_today_offers),

                    onOfferClick = onOfferClick,

                    onOfferSaveClick = onOfferSaveClick,

                    onRetry = onRetry,

                )

            }

            item {

                OfferSection(

                    title = stringResource(R.string.section_coming_soon),

                    status = uiState.comingSoon.status,

                    offers = uiState.comingSoon.items,

                    error = uiState.comingSoon.errorType,

                    emptyTitle = stringResource(R.string.empty_coming_soon),

                    onOfferClick = onOfferClick,

                    onOfferSaveClick = onOfferSaveClick,

                    onRetry = onRetry,

                )

            }

            item {

                ShopSection(

                    status = uiState.nearbyShops.status,

                    shops = uiState.nearbyShops.items,

                    error = uiState.nearbyShops.errorType,

                    onShopClick = onShopClick,

                    onShopSaveClick = onShopSaveClick,

                    onRetry = onRetry,

                )

            }

            item {

                MapCtaCard(onViewMap = onMapClick)

            }

        }

    }

}



@Composable

private fun OfferSection(

    title: String,

    status: SectionStatus,

    offers: List<Offer>,

    error: AppError?,

    emptyTitle: String,

    onOfferClick: (String) -> Unit,

    onOfferSaveClick: (String) -> Unit,

    onRetry: () -> Unit,

) {

    SectionHeader(title = title)

    when (status) {

        SectionStatus.Loading -> OfferCardsSkeleton()

        SectionStatus.Empty -> EmptyState(

            title = emptyTitle,

            hint = stringResource(R.string.empty_hint),

        )

        SectionStatus.Error -> ErrorState(
            error = error,
            onRetry = onRetry,
            fallbackMessageResId = R.string.error_home,
        )

        SectionStatus.Loaded -> {

            LazyRow(

                contentPadding = PaddingValues(horizontal = AasPasSpacing.lg),

                horizontalArrangement = Arrangement.spacedBy(AasPasSpacing.md),

            ) {

                items(offers, key = { it.id }) { offer ->

                    OfferCard(

                        offer = offer,

                        onClick = { onOfferClick(offer.id) },

                        onSaveClick = { onOfferSaveClick(offer.id) },

                    )

                }

            }

        }

    }

}



@Composable

private fun ShopSection(

    status: SectionStatus,

    shops: List<Shop>,

    error: AppError?,

    onShopClick: (String) -> Unit,

    onShopSaveClick: (String) -> Unit,

    onRetry: () -> Unit,

) {

    SectionHeader(title = stringResource(R.string.section_nearby_shops))

    when (status) {

        SectionStatus.Loading -> ShopCardsSkeleton()

        SectionStatus.Empty -> EmptyState(

            title = stringResource(R.string.empty_nearby_shops),

            hint = stringResource(R.string.empty_hint),

        )

        SectionStatus.Error -> ErrorState(
            error = error,
            onRetry = onRetry,
            fallbackMessageResId = R.string.error_home,
        )

        SectionStatus.Loaded -> {

            shops.forEach { shop ->

                ShopCard(

                    shop = shop,

                    onClick = { onShopClick(shop.id) },

                    onSaveClick = { onShopSaveClick(shop.id) },

                )

            }

        }

    }

}

