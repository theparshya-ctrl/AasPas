package com.aaspas.customer.presentation.shopdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.rememberCoroutineScope
import com.aaspas.customer.core.location.AndroidLocationProvider
import com.aaspas.customer.domain.model.SelectedLocation
import kotlinx.coroutines.launch
import com.aaspas.customer.domain.model.OfferVisibilityStatus
import com.aaspas.customer.domain.model.ShopDetails
import com.aaspas.customer.domain.model.ShopOfferSummary
import com.aaspas.customer.presentation.components.DetailsRemoteImage
import com.aaspas.customer.presentation.components.ExternalOfferDisclaimerBadge
import com.aaspas.customer.presentation.components.ShopPhotoUrlField
import com.aaspas.customer.presentation.components.EmptyState
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.components.SectionHeader
import com.aaspas.customer.presentation.components.StatusBadge
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasRadius
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun ShopDetailsRoute(
    shopId: String,
    onBack: () -> Unit,
    onOfferClick: (String) -> Unit,
    onDirections: (Double?, Double?) -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: ShopDetailsViewModel = viewModel(
        factory = ShopDetailsViewModelFactory(app.shopDetailsRepository, shopId),
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

    LaunchedEffect(shopId, selectedLocation) {
        applyLocation(selectedLocation)
    }

    ShopDetailsScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::retry,
        onOfferClick = onOfferClick,
        onDirections = onDirections,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDetailsScreen(
    uiState: ShopDetailsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOfferClick: (String) -> Unit,
    onDirections: (Double?, Double?) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.shop_details_title)) })
        },
        containerColor = AasPasColors.Background,
    ) { padding ->
        when (uiState.status) {
            DetailsLoadStatus.Loading -> ShopLoading(modifier = Modifier.padding(padding))
            DetailsLoadStatus.NotFound -> {
                EmptyState(
                    title = stringResource(R.string.shop_unavailable_title),
                    hint = stringResource(R.string.shop_unavailable_hint),
                    modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
                )
            }
            DetailsLoadStatus.Error -> {
                ErrorState(
                    error = uiState.error,
                    onRetry = onRetry,
                    fallbackMessageResId = R.string.error_shop_details,
                    modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
                )
            }
            DetailsLoadStatus.Loaded -> {
                val shop = uiState.shop ?: return@Scaffold
                ShopDetailsContent(
                    shop = shop,
                    modifier = Modifier.padding(padding),
                    onOfferClick = onOfferClick,
                    onDirections = onDirections,
                )
            }
        }
    }
}

@Composable
private fun ShopDetailsContent(
    shop: ShopDetails,
    modifier: Modifier = Modifier,
    onOfferClick: (String) -> Unit,
    onDirections: (Double?, Double?) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        DetailsRemoteImage(
            photoUrl = shop.photoUrl,
            contentDescription = stringResource(R.string.shop_image_desc, shop.name),
        )
        Column(
            modifier = Modifier.padding(AasPasSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
        ) {
            Text(
                text = shop.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AasPasColors.TextPrimary,
            )
            shop.category?.let {
                Text(text = it, style = MaterialTheme.typography.bodyLarge, color = AasPasColors.TextSecondary)
            }
            if (shop.isVerified) {
                Text(
                    text = stringResource(R.string.verified_shop),
                    style = MaterialTheme.typography.labelLarge,
                    color = AasPasColors.GreenPositive,
                )
            }
            val addressParts = listOfNotNull(
                shop.addressLine1,
                shop.addressLine2,
                shop.area,
                shop.city,
                shop.pincode,
            ).filter { it.isNotBlank() }
            if (addressParts.isNotEmpty()) {
                Text(
                    text = addressParts.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AasPasColors.TextSecondary,
                )
            }
            shop.distanceKm?.let {
                Text(
                    text = stringResource(R.string.distance_km, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AasPasColors.TextSecondary,
                )
            }
            shop.phone?.let {
                Text(
                    text = stringResource(R.string.shop_phone, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AasPasColors.TextPrimary,
                )
            }
            shop.businessHours?.let { hours ->
                val opens = hours.opensAt.orEmpty()
                val closes = hours.closesAt.orEmpty()
                if (opens.isNotBlank() && closes.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.shop_hours, opens, closes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AasPasColors.TextSecondary,
                    )
                }
            }
            Button(
                onClick = { onDirections(shop.latitude, shop.longitude) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.directions_cta))
            }
            if (shop.todayOffers.isNotEmpty()) {
                SectionHeader(title = stringResource(R.string.section_today_offers))
                shop.todayOffers.forEach { offer ->
                    ShopOfferRow(offer = offer, onClick = { onOfferClick(offer.id) })
                }
            }
            if (shop.comingSoon.isNotEmpty()) {
                SectionHeader(title = stringResource(R.string.section_coming_soon))
                shop.comingSoon.forEach { offer ->
                    ShopOfferRow(offer = offer, onClick = { onOfferClick(offer.id) })
                }
            }
            if (shop.todayOffers.isEmpty() && shop.comingSoon.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.empty_today_offers),
                    hint = stringResource(R.string.empty_hint),
                )
            }
        }
    }
}

@Composable
private fun ShopOfferRow(
    offer: ShopOfferSummary,
    onClick: () -> Unit,
) {
    val isComingSoon = offer.status == OfferVisibilityStatus.COMING_SOON
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AasPasSpacing.xs),
        colors = CardDefaults.cardColors(containerColor = AasPasColors.Surface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(AasPasRadius.md),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(AasPasSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs),
        ) {
            Text(
                text = DateFormatters.formatOfferValue(offer.discountType, offer.discountValue),
                style = MaterialTheme.typography.titleMedium,
                color = AasPasColors.PurplePrimary,
            )
            Text(text = offer.title, style = MaterialTheme.typography.bodyLarge)
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
        }
    }
}

@Composable
private fun ShopLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = AasPasColors.PurplePrimary)
    }
}
