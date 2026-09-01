package com.aaspas.customer.presentation.offerdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaspas.customer.AasPasApplication
import com.aaspas.customer.R
import com.aaspas.customer.core.common.DateFormatters
import com.aaspas.customer.domain.model.SelectedLocation
import com.aaspas.customer.core.location.AndroidLocationProvider
import com.aaspas.customer.domain.model.OfferDetails
import com.aaspas.customer.domain.model.OfferVisibilityStatus
import com.aaspas.customer.presentation.components.DetailsRemoteImage
import com.aaspas.customer.presentation.components.EmptyState
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.components.ExternalOfferDisclaimerBadge
import com.aaspas.customer.presentation.components.StatusBadge
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasRadius
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun OfferDetailsRoute(
    offerId: String,
    onBack: () -> Unit,
    onViewShop: (String) -> Unit,
    onDirections: (Double?, Double?) -> Unit,
    onViewCurrentOffers: (String) -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: OfferDetailsViewModel = viewModel(
        factory = OfferDetailsViewModelFactory(app.offerDetailsRepository, offerId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLocation by app.selectedLocationStore.selectedLocation.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationProvider = AndroidLocationProvider(context)
    val scope = rememberCoroutineScope()

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

    LaunchedEffect(offerId, selectedLocation) {
        applyLocation(selectedLocation)
    }

    OfferDetailsScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::retry,
        onViewShop = onViewShop,
        onDirections = onDirections,
        onViewCurrentOffers = onViewCurrentOffers,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferDetailsScreen(
    uiState: OfferDetailsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onViewShop: (String) -> Unit,
    onDirections: (Double?, Double?) -> Unit,
    onViewCurrentOffers: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.offer_details_title)) })
        },
        containerColor = AasPasColors.Background,
    ) { padding ->
        when (uiState.status) {
            DetailsLoadStatus.Loading -> {
                BoxLoading(modifier = Modifier.padding(padding))
            }
            DetailsLoadStatus.NotFound -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(AasPasSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
                ) {
                    EmptyState(
                        title = stringResource(R.string.offer_unavailable_title),
                        hint = stringResource(R.string.offer_unavailable_hint),
                    )
                    Button(onClick = onBack) {
                        Text(stringResource(R.string.view_current_offers))
                    }
                }
            }
            DetailsLoadStatus.Error -> {
                ErrorState(
                    error = uiState.error,
                    onRetry = onRetry,
                    fallbackMessageResId = R.string.error_offer_details,
                    modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
                )
            }
            DetailsLoadStatus.Loaded -> {
                val offer = uiState.offer ?: return@Scaffold
                OfferDetailsContent(
                    offer = offer,
                    modifier = Modifier.padding(padding),
                    onViewShop = onViewShop,
                    onDirections = onDirections,
                    onViewCurrentOffers = onViewCurrentOffers,
                )
            }
        }
    }
}

@Composable
private fun OfferDetailsContent(
    offer: OfferDetails,
    modifier: Modifier = Modifier,
    onViewShop: (String) -> Unit,
    onDirections: (Double?, Double?) -> Unit,
    onViewCurrentOffers: (String) -> Unit,
) {
    val isComingSoon = offer.status == OfferVisibilityStatus.COMING_SOON
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DetailsRemoteImage(
            photoUrl = offer.photoUrl,
            contentDescription = stringResource(R.string.offer_image_desc, offer.title),
        )
        Column(
            modifier = Modifier.padding(AasPasSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
        ) {
            Text(
                text = DateFormatters.formatOfferValue(offer.discountType, offer.discountValue),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AasPasColors.PurplePrimary,
            )
            Text(
                text = offer.title,
                style = MaterialTheme.typography.titleLarge,
                color = AasPasColors.TextPrimary,
            )
            offer.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AasPasColors.TextSecondary,
                )
            }
            StatusBadge(
                text = if (isComingSoon) {
                    stringResource(R.string.coming_soon_badge)
                } else {
                    stringResource(R.string.active_badge)
                },
                isComingSoon = isComingSoon,
            )
            if (offer.isVerified) {
                StatusBadge(
                    text = stringResource(R.string.verified_by_aaspas),
                    isComingSoon = false,
                )
            } else if (offer.isExternal) {
                ExternalOfferDisclaimerBadge(sourceName = offer.sourceName)
            }
            Text(
                text = when {
                    isComingSoon && !offer.startsAt.isNullOrBlank() ->
                        stringResource(R.string.starts_on, DateFormatters.formatShortDate(offer.startsAt))
                    isComingSoon ->
                        stringResource(R.string.coming_soon_badge)
                    !offer.endsAt.isNullOrBlank() ->
                        stringResource(R.string.valid_until, DateFormatters.formatShortDate(offer.endsAt))
                    else ->
                        stringResource(R.string.ongoing_offer)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = AasPasColors.TextSecondary,
            )
            ShopSummaryCard(offer = offer, onViewShop = { onViewShop(offer.shop.id) })
            Button(
                onClick = { onDirections(offer.shop.latitude, offer.shop.longitude) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.directions_cta))
            }
            if (!isComingSoon) {
                Spacer(modifier = Modifier.height(AasPasSpacing.sm))
            } else {
                Button(
                    onClick = { onViewCurrentOffers(offer.shop.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.view_current_offers))
                }
            }
        }
    }
}

@Composable
private fun ShopSummaryCard(
    offer: OfferDetails,
    onViewShop: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AasPasColors.Surface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(AasPasRadius.lg),
    ) {
        Column(
            modifier = Modifier.padding(AasPasSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
        ) {
            Text(
                text = stringResource(R.string.shop_summary_title),
                style = MaterialTheme.typography.labelLarge,
                color = AasPasColors.TextSecondary,
            )
            Text(
                text = offer.shop.name,
                style = MaterialTheme.typography.titleMedium,
                color = AasPasColors.TextPrimary,
            )
            offer.shop.category?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = AasPasColors.TextSecondary)
            }
            offer.shop.addressArea?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = AasPasColors.TextSecondary)
            }
            offer.shop.distanceKm?.let {
                Text(
                    text = stringResource(R.string.distance_km, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AasPasColors.TextSecondary,
                )
            }
            Button(onClick = onViewShop, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.view_shop))
            }
        }
    }
}

@Composable
private fun BoxLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = AasPasColors.PurplePrimary)
    }
}
