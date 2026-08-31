package com.aaspas.customer.presentation.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.aaspas.customer.core.location.AndroidLocationProvider
import com.aaspas.customer.core.auth.SessionState
import com.aaspas.customer.domain.model.SelectedLocation
import com.aaspas.customer.presentation.components.EmptyState
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.components.OfferCard
import com.aaspas.customer.presentation.components.SectionHeader
import com.aaspas.customer.presentation.components.ShopCard
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing
import kotlinx.coroutines.launch

@Composable
fun FavoritesRoute(
    onOfferClick: (String) -> Unit,
    onShopClick: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: FavoritesViewModel = viewModel(
        factory = FavoritesViewModelFactory(app.favoritesRepository, app.authRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLocation by app.selectedLocationStore.selectedLocation.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationProvider = AndroidLocationProvider(context)
    val sessionState by app.authRepository.sessionState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var initialLocationApplied by remember { mutableStateOf(false) }

    fun applyLocation(location: SelectedLocation) {
        if (location.hasCoordinates) {
            viewModel.load(location.latitude, location.longitude)
        } else if (locationProvider.hasLocationPermission()) {
            scope.launch {
                val coords = locationProvider.getLastLocation()
                viewModel.load(coords?.latitude, coords?.longitude)
            }
        } else {
            viewModel.load(null, null)
        }
    }

    LaunchedEffect(Unit) {
        applyLocation(selectedLocation)
        initialLocationApplied = true
    }

    LaunchedEffect(selectedLocation, sessionState) {
        if (!initialLocationApplied) return@LaunchedEffect
        applyLocation(selectedLocation)
    }

    FavoritesScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onSignInClick = onNavigateToLogin,
        onOfferClick = onOfferClick,
        onShopClick = onShopClick,
        onUnsaveOffer = viewModel::unsaveOffer,
        onUnsaveShop = viewModel::unsaveShop,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    uiState: FavoritesUiState,
    onRetry: () -> Unit,
    onSignInClick: () -> Unit,
    onOfferClick: (String) -> Unit,
    onShopClick: (String) -> Unit,
    onUnsaveOffer: (String) -> Unit,
    onUnsaveShop: (String) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_favorites)) }) },
        containerColor = AasPasColors.Background,
    ) { padding ->
        when (uiState.status) {
            FavoritesLoadStatus.Loading -> FavoritesLoading(Modifier.padding(padding))
            FavoritesLoadStatus.Unauthorized -> {
                Column(
                    modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
                ) {
                    EmptyState(
                        title = stringResource(R.string.login_required_title),
                        hint = stringResource(R.string.login_required_message),
                    )
                    Button(onClick = onSignInClick) {
                        Text(stringResource(R.string.sign_in))
                    }
                }
            }
            FavoritesLoadStatus.Empty -> EmptyState(
                title = stringResource(R.string.favorites_empty_title),
                hint = stringResource(R.string.favorites_empty_hint),
                modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
            )
            FavoritesLoadStatus.Error -> ErrorState(
                error = uiState.error,
                onRetry = onRetry,
                fallbackMessageResId = R.string.error_favorites,
                modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
            )
            FavoritesLoadStatus.Loaded -> {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(bottom = AasPasSpacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
                ) {
                    if (uiState.offers.isNotEmpty()) {
                        item { SectionHeader(title = stringResource(R.string.section_saved_offers)) }
                        items(uiState.offers, key = { it.favoriteId }) { favorite ->
                            Column(modifier = Modifier.padding(horizontal = AasPasSpacing.lg)) {
                                if (!favorite.isActive) {
                                    Text(
                                        text = stringResource(R.string.offer_unavailable_title),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AasPasColors.AmberAttention,
                                        modifier = Modifier.padding(bottom = AasPasSpacing.xs),
                                    )
                                }
                                OfferCard(
                                    offer = favorite.offer,
                                    onClick = { onOfferClick(favorite.offer.id) },
                                    onSaveClick = { onUnsaveOffer(favorite.offer.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    if (uiState.shops.isNotEmpty()) {
                        item { SectionHeader(title = stringResource(R.string.section_saved_shops)) }
                        items(uiState.shops, key = { it.favoriteId }) { favorite ->
                            Column {
                                if (!favorite.isActive) {
                                    Text(
                                        text = stringResource(R.string.shop_unavailable_title),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AasPasColors.AmberAttention,
                                        modifier = Modifier.padding(horizontal = AasPasSpacing.lg),
                                    )
                                }
                                ShopCard(
                                    shop = favorite.shop,
                                    onClick = { onShopClick(favorite.shop.id) },
                                    onSaveClick = { onUnsaveShop(favorite.shop.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = AasPasColors.PurplePrimary)
    }
}
