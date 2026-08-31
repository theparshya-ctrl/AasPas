package com.aaspas.customer.presentation.admin

import androidx.compose.foundation.clickable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaspas.customer.AasPasApplication
import com.aaspas.customer.R
import com.aaspas.customer.core.common.DateFormatters
import com.aaspas.customer.domain.model.AdminShopReview
import com.aaspas.customer.presentation.components.EmptyState
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun AdminShopVerificationRoute(
    onBack: () -> Unit,
    onReviewShop: (String) -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: AdminShopVerificationViewModel = viewModel(
        factory = AdminShopVerificationViewModelFactory(app.adminRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    AdminShopVerificationScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::load,
        onReviewShop = onReviewShop,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminShopVerificationScreen(
    uiState: AdminShopVerificationUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onReviewShop: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_shop_verification)) },
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
            AdminShopVerificationStatus.Loading -> {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator(color = AasPasColors.PurplePrimary) }
            }
            AdminShopVerificationStatus.Error -> {
                ErrorState(error = uiState.error, onRetry = onRetry, modifier = Modifier.padding(padding).padding(AasPasSpacing.lg))
            }
            AdminShopVerificationStatus.Loaded -> {
                if (uiState.shops.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.admin_shop_queue_empty_title),
                        hint = stringResource(R.string.admin_shop_queue_empty_hint),
                        modifier = Modifier.padding(padding).padding(AasPasSpacing.lg),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(padding).fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(AasPasSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
                    ) {
                        items(uiState.shops, key = { it.shopId }) { shop ->
                            PendingShopCard(shop = shop, onClick = { onReviewShop(shop.shopId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingShopCard(
    shop: AdminShopReview,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(AasPasSpacing.md), verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs)) {
            Text(shop.shopName, style = MaterialTheme.typography.titleMedium)
            shop.category?.let { Text(it, color = AasPasColors.TextSecondary) }
            shop.area?.let { Text(it, color = AasPasColors.TextSecondary) }
            Text(
                stringResource(R.string.admin_shop_queue_owner, shop.owner.fullName ?: shop.owner.email),
                style = MaterialTheme.typography.bodyMedium,
            )
            shop.submittedAt?.let {
                Text(
                    stringResource(R.string.admin_queue_submitted, DateFormatters.formatShortDate(it)),
                    color = AasPasColors.TextSecondary,
                )
            }
            Text(stringResource(R.string.shop_owner_status_pending), color = AasPasColors.PurplePrimary)
        }
    }
}
