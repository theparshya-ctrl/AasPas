package com.aaspas.customer.presentation.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.aaspas.customer.domain.model.AdminShopListItem
import com.aaspas.customer.presentation.components.EmptyState
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun AdminShopManagementRoute(
    onBack: () -> Unit,
    onOpenShop: (String) -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: AdminShopManagementViewModel = viewModel(
        factory = AdminShopManagementViewModelFactory(app.adminRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    AdminShopManagementScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::load,
        onFilterSelected = viewModel::onFilterSelected,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSearchSubmit = viewModel::submitSearch,
        onOpenShop = onOpenShop,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminShopManagementScreen(
    uiState: AdminShopManagementUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onFilterSelected: (AdminShopFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onOpenShop: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_shop_management)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        containerColor = AasPasColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(AasPasSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.admin_shop_search_hint)) },
                singleLine = true,
                trailingIcon = {
                    TextButton(onClick = onSearchSubmit) {
                        Text(stringResource(R.string.search))
                    }
                },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AasPasSpacing.xs)) {
                AdminShopFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    AdminShopFilter.ALL -> stringResource(R.string.admin_filter_all)
                                    AdminShopFilter.PENDING -> stringResource(R.string.admin_filter_pending)
                                    AdminShopFilter.ACTIVE -> stringResource(R.string.admin_filter_active)
                                    AdminShopFilter.REJECTED -> stringResource(R.string.admin_filter_rejected)
                                },
                            )
                        },
                    )
                }
            }
            when (uiState.status) {
                AdminShopManagementStatus.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) { CircularProgressIndicator(color = AasPasColors.PurplePrimary) }
                }
                AdminShopManagementStatus.Error -> {
                    ErrorState(error = uiState.error, onRetry = onRetry)
                }
                AdminShopManagementStatus.Loaded -> {
                    if (uiState.shops.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.admin_shop_management_empty_title),
                            hint = stringResource(R.string.admin_shop_management_empty_hint),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
                        ) {
                            items(uiState.shops, key = { it.shopId }) { shop ->
                                AdminShopManagementCard(shop = shop, onClick = { onOpenShop(shop.shopId) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminShopManagementCard(
    shop: AdminShopListItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(AasPasSpacing.md), verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs)) {
            Text(shop.shopName, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.admin_shop_queue_owner, shop.ownerName ?: shop.ownerEmail),
                color = AasPasColors.TextSecondary,
            )
            shop.category?.let { Text(it, color = AasPasColors.TextSecondary) }
            shop.city?.let { Text(it, color = AasPasColors.TextSecondary) }
            Text(
                stringResource(R.string.admin_shop_management_meta, shop.status, shop.offerCount),
                color = AasPasColors.TextSecondary,
            )
            Text(
                stringResource(R.string.admin_shop_management_created, DateFormatters.formatShortDate(shop.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = AasPasColors.TextSecondary,
            )
            shop.rejectionReason?.let {
                Text(
                    stringResource(R.string.admin_field_previous_rejection) + ": " + it,
                    color = AasPasColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
