package com.aaspas.customer.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.aaspas.customer.domain.model.AdminShopDetail
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun AdminShopManagementDetailRoute(
    shopId: String,
    onBack: () -> Unit,
    onOpenVerificationReview: (String) -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: AdminShopManagementDetailViewModel = viewModel(
        factory = AdminShopManagementDetailViewModelFactory(app.adminRepository, shopId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(shopId) { viewModel.load() }

    AdminShopManagementDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::load,
        onOpenVerificationReview = onOpenVerificationReview,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminShopManagementDetailScreen(
    uiState: AdminShopManagementDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenVerificationReview: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_shop_management_detail_title)) },
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
            AdminShopManagementDetailStatus.Loading -> {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator(color = AasPasColors.PurplePrimary) }
            }
            AdminShopManagementDetailStatus.Error -> {
                ErrorState(error = uiState.error, onRetry = onRetry, modifier = Modifier.padding(padding).padding(AasPasSpacing.lg))
            }
            AdminShopManagementDetailStatus.Loaded -> {
                val detail = uiState.detail ?: return@Scaffold
                AdminShopManagementDetailContent(
                    detail = detail,
                    modifier = Modifier.padding(padding),
                    onOpenVerificationReview = onOpenVerificationReview,
                )
            }
        }
    }
}

@Composable
private fun AdminShopManagementDetailContent(
    detail: AdminShopDetail,
    modifier: Modifier = Modifier,
    onOpenVerificationReview: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AasPasSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(AasPasSpacing.md), verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs)) {
                Text(stringResource(R.string.admin_shop_info_section), style = MaterialTheme.typography.titleMedium)
                DetailRow(stringResource(R.string.admin_field_shop), detail.shopName)
                detail.category?.let { DetailRow(stringResource(R.string.admin_field_category), it) }
                detail.description?.let { DetailRow(stringResource(R.string.shop_owner_description), it) }
                detail.city?.let { DetailRow(stringResource(R.string.shop_owner_city), it) }
                detail.area?.let { DetailRow(stringResource(R.string.shop_owner_area), it) }
                DetailRow(stringResource(R.string.shop_owner_approval_status), detail.status)
                DetailRow(stringResource(R.string.admin_field_shop_verification), if (detail.isVerified) "Verified" else "Not verified")
                Text(
                    stringResource(R.string.admin_shop_management_created, DateFormatters.formatShortDate(detail.createdAt)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(AasPasSpacing.md), verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs)) {
                Text(stringResource(R.string.admin_shop_owner_section), style = MaterialTheme.typography.titleMedium)
                DetailRow(stringResource(R.string.admin_field_merchant), detail.owner.fullName ?: detail.owner.email)
                DetailRow(stringResource(R.string.email_label), detail.owner.email)
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(AasPasSpacing.md), verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs)) {
                Text(stringResource(R.string.admin_shop_management_offers_section), style = MaterialTheme.typography.titleMedium)
                DetailRow(stringResource(R.string.admin_shop_management_offer_count), detail.offerCount.toString())
                detail.offerCountsByStatus.filter { it.value > 0 }.forEach { (status, count) ->
                    DetailRow(status.replace('_', ' '), count.toString())
                }
            }
        }
        if (detail.auditEntries.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(AasPasSpacing.md), verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs)) {
                    Text(stringResource(R.string.admin_shop_management_audit_section), style = MaterialTheme.typography.titleMedium)
                    detail.auditEntries.forEach { entry ->
                        Text(
                            buildString {
                                append(DateFormatters.formatShortDate(entry.createdAt))
                                entry.message?.let { append(": $it") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = AasPasColors.TextSecondary,
                        )
                    }
                }
            }
        }
        if (detail.status == "pending_approval") {
            OutlinedButton(
                onClick = { onOpenVerificationReview(detail.shopId) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.admin_shop_management_open_verification))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        return
    }
    Text("$label: $value", style = MaterialTheme.typography.bodyMedium)
}
