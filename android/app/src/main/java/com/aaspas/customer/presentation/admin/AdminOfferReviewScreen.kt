package com.aaspas.customer.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.aaspas.customer.data.mapper.AdminMapper
import com.aaspas.customer.domain.model.AdminOfferReview
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.components.OfferCard
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun AdminOfferReviewRoute(
    offerId: String,
    onBack: () -> Unit,
    onCompleted: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: AdminOfferReviewViewModel = viewModel(
        factory = AdminOfferReviewViewModelFactory(app.adminRepository, offerId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.status) {
        if (uiState.status == AdminOfferReviewStatus.Approved || uiState.status == AdminOfferReviewStatus.Rejected) {
            onCompleted()
        }
    }

    AdminOfferReviewScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::load,
        onApproveClick = viewModel::showApproveDialog,
        onRejectClick = viewModel::showRejectDialog,
        onConfirmApprove = viewModel::approveOffer,
        onDismissApprove = viewModel::hideApproveDialog,
        onConfirmReject = viewModel::rejectOffer,
        onDismissReject = viewModel::hideRejectDialog,
        onRejectReasonChange = viewModel::onRejectReasonChange,
        onRejectPresetSelected = viewModel::onRejectPresetSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOfferReviewScreen(
    uiState: AdminOfferReviewUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit,
    onConfirmApprove: () -> Unit,
    onDismissApprove: () -> Unit,
    onConfirmReject: () -> Unit,
    onDismissReject: () -> Unit,
    onRejectReasonChange: (String) -> Unit,
    onRejectPresetSelected: (String) -> Unit,
) {
    if (uiState.showApproveDialog) {
        AlertDialog(
            onDismissRequest = onDismissApprove,
            title = { Text(stringResource(R.string.admin_approve_confirm_title)) },
            text = { Text(stringResource(R.string.admin_approve_confirm_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmApprove) {
                    Text(stringResource(R.string.admin_approve_action))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissApprove) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    if (uiState.showRejectDialog) {
        AlertDialog(
            onDismissRequest = onDismissReject,
            title = { Text(stringResource(R.string.admin_reject_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm)) {
                    RejectPresetChips(onSelect = onRejectPresetSelected)
                    OutlinedTextField(
                        value = uiState.rejectReason,
                        onValueChange = onRejectReasonChange,
                        label = { Text(stringResource(R.string.admin_reject_reason_label)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    uiState.validationMessage?.let {
                        Text(it, color = AasPasColors.RedCritical)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmReject) {
                    Text(stringResource(R.string.admin_reject_action))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissReject) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_review_title)) },
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
            AdminOfferReviewStatus.Loading,
            AdminOfferReviewStatus.Approving,
            AdminOfferReviewStatus.Rejecting,
            -> {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator(color = AasPasColors.PurplePrimary) }
            }
            AdminOfferReviewStatus.Error -> {
                ErrorState(error = uiState.error, onRetry = onRetry, modifier = Modifier.padding(padding).padding(AasPasSpacing.lg))
            }
            AdminOfferReviewStatus.Loaded,
            AdminOfferReviewStatus.Approved,
            AdminOfferReviewStatus.Rejected,
            -> {
                val review = uiState.review ?: return@Scaffold
                AdminOfferReviewContent(
                    review = review,
                    modifier = Modifier.padding(padding),
                    onApproveClick = onApproveClick,
                    onRejectClick = onRejectClick,
                    actionsEnabled = uiState.status == AdminOfferReviewStatus.Loaded,
                )
            }
        }
    }
}

@Composable
private fun AdminOfferReviewContent(
    review: AdminOfferReview,
    modifier: Modifier = Modifier,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit,
    actionsEnabled: Boolean,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AasPasSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
    ) {
        Text(stringResource(R.string.admin_customer_preview), style = MaterialTheme.typography.titleMedium)
        OfferCard(offer = AdminMapper.toPreviewOffer(review), onClick = {})
        Text(stringResource(R.string.admin_review_info), style = MaterialTheme.typography.titleMedium)
        AdminInfoRow(stringResource(R.string.admin_field_shop), review.shop.shopName)
        review.shop.category?.let { AdminInfoRow(stringResource(R.string.admin_field_category), it) }
        AdminInfoRow(
            stringResource(R.string.admin_field_shop_verification),
            if (review.shop.isVerified) {
                stringResource(R.string.shop_owner_verified)
            } else {
                stringResource(R.string.shop_owner_not_verified)
            },
        )
        AdminInfoRow(
            stringResource(R.string.admin_field_merchant),
            review.merchant.fullName?.let { "$it (${review.merchant.email})" } ?: review.merchant.email,
        )
        review.submittedAt?.let {
            AdminInfoRow(stringResource(R.string.admin_field_submitted), DateFormatters.formatShortDate(it))
        }
        review.merchantConfirmedAt?.let {
            AdminInfoRow(stringResource(R.string.admin_field_merchant_confirmed), DateFormatters.formatShortDate(it))
        }
        review.applicableProducts?.takeIf { it.isNotBlank() }?.let {
            AdminInfoRow(stringResource(R.string.offer_field_applicable_products), it)
        }
        review.terms?.takeIf { it.isNotBlank() }?.let {
            AdminInfoRow(stringResource(R.string.offer_field_terms), it)
        }
        review.startsAt?.let { start ->
            AdminInfoRow(
                stringResource(R.string.admin_field_validity),
                "${DateFormatters.formatShortDate(start)} – ${review.endsAt?.let { DateFormatters.formatShortDate(it) }.orEmpty()}",
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AasPasSpacing.sm)) {
            Button(onClick = onApproveClick, enabled = actionsEnabled, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.admin_approve_action))
            }
            OutlinedButton(onClick = onRejectClick, enabled = actionsEnabled, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.admin_reject_action))
            }
        }
    }
}

@Composable
private fun AdminInfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AasPasColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RejectPresetChips(onSelect: (String) -> Unit) {
    val presets = listOf(
        stringResource(R.string.admin_reject_unclear),
        stringResource(R.string.admin_reject_discount),
        stringResource(R.string.admin_reject_terms),
        stringResource(R.string.admin_reject_image),
        stringResource(R.string.admin_reject_unacceptable),
        stringResource(R.string.admin_reject_other),
    )
    Column(verticalArrangement = Arrangement.spacedBy(AasPasSpacing.xs)) {
        presets.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(AasPasSpacing.xs)) {
                row.forEach { label ->
                    FilterChip(
                        selected = false,
                        onClick = { onSelect(label) },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}
