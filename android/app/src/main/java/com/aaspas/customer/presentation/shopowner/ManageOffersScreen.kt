package com.aaspas.customer.presentation.shopowner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaspas.customer.AasPasApplication
import com.aaspas.customer.R
import com.aaspas.customer.core.common.DateFormatters
import com.aaspas.customer.domain.model.MerchantOffer
import com.aaspas.customer.domain.model.MerchantOfferStatus
import com.aaspas.customer.domain.shopowner.MerchantOfferValidator
import com.aaspas.customer.presentation.components.EmptyState
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun ManageOffersRoute(
    shopId: String,
    onBack: () -> Unit,
    onEditOffer: (String) -> Unit,
    onViewOffer: (String) -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: ManageOffersViewModel = viewModel(
        factory = ManageOffersViewModelFactory(app.shopOwnerRepository, shopId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) { viewModel.load() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ManageOffersScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::load,
        onEditOffer = onEditOffer,
        onViewOffer = onViewOffer,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageOffersScreen(
    uiState: ManageOffersUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEditOffer: (String) -> Unit,
    onViewOffer: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shop_owner_manage_offers)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        containerColor = AasPasColors.Background,
    ) { padding ->
        when (uiState.status) {
            ManageOffersStatus.Loading -> {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator(color = AasPasColors.PurplePrimary) }
            }
            ManageOffersStatus.Error -> {
                ErrorState(error = uiState.error, onRetry = onRetry, modifier = Modifier.padding(padding).padding(AasPasSpacing.lg))
            }
            ManageOffersStatus.Loaded -> {
                if (uiState.offers.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.manage_offers_empty_title),
                        hint = stringResource(R.string.manage_offers_empty_hint),
                        modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(padding).fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(AasPasSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
                    ) {
                        if (uiState.shopName.isNotBlank()) {
                            item(key = "shop-header") {
                                Text(
                                    text = stringResource(R.string.manage_offer_shop, uiState.shopName),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AasPasColors.TextSecondary,
                                    modifier = Modifier.padding(bottom = AasPasSpacing.xs),
                                )
                            }
                        }
                        items(uiState.offers, key = { it.id }) { offer ->
                            ManageOfferCard(
                                offer = offer,
                                shopName = uiState.shopName,
                                onEditOffer = onEditOffer,
                                onViewOffer = onViewOffer,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManageOfferCard(
    offer: MerchantOffer,
    shopName: String,
    onEditOffer: (String) -> Unit,
    onViewOffer: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AasPasSpacing.md), verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs)) {
            Text(offer.title, style = MaterialTheme.typography.titleMedium)
            if (shopName.isNotBlank()) {
                Text(
                    text = stringResource(R.string.manage_offer_shop, shopName),
                    style = MaterialTheme.typography.bodySmall,
                    color = AasPasColors.TextSecondary,
                )
            }
            Text(stringResource(R.string.manage_offer_status, offerStatusLabel(offer)), color = AasPasColors.TextSecondary)
            Text(stringResource(R.string.manage_offer_verification, verificationLabel(offer)))
            offer.startsAt?.let { start ->
                Text(
                    stringResource(
                        R.string.manage_offer_dates,
                        DateFormatters.formatShortDate(start),
                        offer.endsAt?.let { DateFormatters.formatShortDate(it) }.orEmpty(),
                    ),
                )
            }
            Text(stringResource(R.string.manage_offer_discount, formatDiscountType(offer.discountType), offer.discountValue))
            offer.rejectionReason?.takeIf { offer.status == MerchantOfferStatus.Rejected }?.let {
                Text(stringResource(R.string.manage_offer_rejection, it), color = AasPasColors.RedCritical)
            }
            when {
                MerchantOfferValidator.isEditable(offer.status) -> {
                    OutlinedButton(onClick = { onEditOffer(offer.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(
                                if (offer.status == MerchantOfferStatus.Rejected) {
                                    R.string.manage_offer_edit_resubmit
                                } else {
                                    R.string.manage_offer_edit_draft
                                },
                            ),
                        )
                    }
                }
                else -> {
                    OutlinedButton(onClick = { onViewOffer(offer.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.manage_offer_view))
                    }
                }
            }
        }
    }
}

@Composable
private fun offerStatusLabel(offer: MerchantOffer): String {
    return when (offer.status) {
        MerchantOfferStatus.Draft -> stringResource(R.string.offer_status_draft)
        MerchantOfferStatus.PendingApproval -> stringResource(R.string.offer_status_pending)
        MerchantOfferStatus.Rejected -> stringResource(R.string.offer_status_rejected)
        MerchantOfferStatus.Scheduled -> {
            if (offer.isVerified) {
                stringResource(R.string.offer_status_scheduled_approved)
            } else {
                stringResource(R.string.offer_status_scheduled)
            }
        }
        MerchantOfferStatus.Active -> stringResource(R.string.offer_status_active)
        MerchantOfferStatus.Expired -> stringResource(R.string.offer_status_expired)
        MerchantOfferStatus.Unknown -> stringResource(R.string.shop_owner_status_unknown)
    }
}

@Composable
private fun verificationLabel(offer: MerchantOffer): String {
    return when {
        offer.status == MerchantOfferStatus.PendingApproval ->
            stringResource(R.string.manage_offer_verification_pending)
        offer.isVerified ->
            stringResource(R.string.manage_offer_verification_verified)
        else ->
            stringResource(R.string.manage_offer_verification_not_verified)
    }
}

@Composable
private fun formatDiscountType(type: String): String {
    return when (type) {
        "percentage" -> stringResource(R.string.offer_discount_percentage)
        "fixed" -> stringResource(R.string.offer_discount_fixed)
        else -> type
    }
}
