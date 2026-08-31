package com.aaspas.customer.presentation.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaspas.customer.AasPasApplication
import com.aaspas.customer.R
import com.aaspas.customer.domain.model.SelectedLocation
import com.aaspas.customer.core.location.AndroidLocationProvider
import com.aaspas.customer.presentation.components.EmptyState
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.components.OfferCard
import com.aaspas.customer.presentation.components.SectionHeader
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing
import kotlinx.coroutines.launch

@Composable
fun CategoryOffersRoute(
    categoryId: String,
    categoryName: String,
    onOfferClick: (String) -> Unit,
    onMapClick: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: CategoryOffersViewModel = viewModel(
        factory = CategoryOffersViewModelFactory(
            app.categoryOffersRepository,
            categoryId,
            categoryName,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLocation by app.selectedLocationStore.selectedLocation.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationProvider = AndroidLocationProvider(context)
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

    LaunchedEffect(categoryId) {
        applyLocation(selectedLocation)
        initialLocationApplied = true
    }

    LaunchedEffect(selectedLocation) {
        if (!initialLocationApplied) return@LaunchedEffect
        applyLocation(selectedLocation)
    }

    CategoryOffersScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onOfferClick = onOfferClick,
        onMapClick = onMapClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryOffersScreen(
    uiState: CategoryOffersUiState,
    onRetry: () -> Unit,
    onOfferClick: (String) -> Unit,
    onMapClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(uiState.categoryName) })
        },
        containerColor = AasPasColors.Background,
    ) { padding ->
        when (uiState.status) {
            CategoryLoadStatus.Loading -> CategoryLoading(modifier = Modifier.padding(padding))
            CategoryLoadStatus.Empty -> EmptyState(
                title = stringResource(R.string.empty_today_offers),
                hint = stringResource(R.string.empty_hint),
                modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
            )
            CategoryLoadStatus.Error -> ErrorState(
                error = uiState.error,
                onRetry = onRetry,
                fallbackMessageResId = R.string.error_category,
                modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
            )
            CategoryLoadStatus.Loaded -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(bottom = AasPasSpacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
                ) {
                    item {
                        Column(modifier = Modifier.padding(AasPasSpacing.lg)) {
                            Text(
                                text = uiState.categoryName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = AasPasColors.TextPrimary,
                            )
                            if (uiState.totalActive > 0) {
                                Text(
                                    text = stringResource(R.string.category_offer_count, uiState.totalActive),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AasPasColors.TextSecondary,
                                )
                            }
                            OutlinedButton(
                                onClick = onMapClick,
                                modifier = Modifier.padding(top = AasPasSpacing.sm),
                            ) {
                                Text(text = stringResource(R.string.map_cta_button))
                            }
                        }
                    }
                    if (uiState.todayOffers.isNotEmpty()) {
                        item {
                            SectionHeader(title = stringResource(R.string.section_today_offers))
                        }
                        items(uiState.todayOffers, key = { it.id }) { offer ->
                            OfferCard(
                                offer = offer,
                                onClick = { onOfferClick(offer.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AasPasSpacing.lg),
                            )
                        }
                    }
                    if (uiState.comingSoon.isNotEmpty()) {
                        item {
                            SectionHeader(title = stringResource(R.string.section_coming_soon))
                        }
                        items(uiState.comingSoon, key = { it.id }) { offer ->
                            OfferCard(
                                offer = offer,
                                onClick = { onOfferClick(offer.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AasPasSpacing.lg),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = AasPasColors.PurplePrimary)
    }
}
