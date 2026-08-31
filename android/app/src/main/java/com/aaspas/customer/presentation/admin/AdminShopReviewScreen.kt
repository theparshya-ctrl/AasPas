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
import com.aaspas.customer.domain.model.AdminShopReview
import com.aaspas.customer.presentation.components.ErrorState
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun AdminShopReviewRoute(
    shopId: String,
    onBack: () -> Unit,
    onCompleted: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: AdminShopReviewViewModel = viewModel(
        factory = AdminShopReviewViewModelFactory(app.adminRepository, shopId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.status) {
        if (uiState.status == AdminShopReviewStatus.Approved || uiState.status == AdminShopReviewStatus.Rejected) {
            onCompleted()
        }
    }

    AdminShopReviewScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::load,
        onApproveClick = viewModel::showApproveDialog,
        onRejectClick = viewModel::showRejectDialog,
        onConfirmApprove = viewModel::approveShop,
        onDismissApprove = viewModel::hideApproveDialog,
        onConfirmReject = viewModel::rejectShop,
        onDismissReject = viewModel::hideRejectDialog,
        onRejectReasonChange = viewModel::onRejectReasonChange,
        onRejectPresetSelected = viewModel::onRejectPresetSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminShopReviewScreen(
    uiState: AdminShopReviewUiState,
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
        val review = uiState.review
        AlertDialog(
            onDismissRequest = onDismissApprove,
            title = { Text(stringResource(R.string.admin_shop_approve_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.admin_shop_approve_confirm_message,
                        review?.shopName.orEmpty(),
                        review?.owner?.fullName ?: review?.owner?.email.orEmpty(),
                    ),
                )
            },
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
            title = { Text(stringResource(R.string.admin_shop_reject_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm)) {
                    ShopRejectPresetChips(onSelect = onRejectPresetSelected)
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
                TextButton(
                    onClick = onConfirmReject,
                    enabled = uiState.rejectReason.trim().length >= 3,
                ) {
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
                title = { Text(stringResource(R.string.admin_shop_review_title)) },
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
            AdminShopReviewStatus.Loading,
            AdminShopReviewStatus.Approving,
            AdminShopReviewStatus.Rejecting,
            -> {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator(color = AasPasColors.PurplePrimary) }
            }
            AdminShopReviewStatus.Error -> {
                ErrorState(error = uiState.error, onRetry = onRetry, modifier = Modifier.padding(padding).padding(AasPasSpacing.lg))
            }
            AdminShopReviewStatus.Loaded,
            AdminShopReviewStatus.Approved,
            AdminShopReviewStatus.Rejected,
            -> {
                val review = uiState.review ?: return@Scaffold
                AdminShopReviewContent(
                    review = review,
                    modifier = Modifier.padding(padding),
                    onApproveClick = onApproveClick,
                    onRejectClick = onRejectClick,
                    actionsEnabled = uiState.status == AdminShopReviewStatus.Loaded,
                )
            }
        }
    }
}

@Composable
private fun AdminShopReviewContent(
    review: AdminShopReview,
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
        Text(stringResource(R.string.admin_shop_info_section), style = MaterialTheme.typography.titleMedium)
        AdminInfoRow(stringResource(R.string.admin_field_shop), review.shopName)
        review.category?.let { AdminInfoRow(stringResource(R.string.admin_field_category), it) }
        review.description?.let { AdminInfoRow(stringResource(R.string.shop_owner_description), it) }
        review.photoUrl?.let { AdminInfoRow(stringResource(R.string.shop_owner_photo), it) }
        review.addressLine1?.let { AdminInfoRow(stringResource(R.string.shop_owner_address_line1), it) }
        review.area?.let { AdminInfoRow(stringResource(R.string.shop_owner_area), it) }
        review.city?.let { AdminInfoRow(stringResource(R.string.shop_owner_city), it) }
        review.pincode?.let { AdminInfoRow(stringResource(R.string.shop_owner_pincode), it) }
        if (review.latitude != null && review.longitude != null) {
            AdminInfoRow(
                stringResource(R.string.shop_owner_location),
                "${review.latitude}, ${review.longitude}",
            )
        }
        review.businessHours?.let {
            AdminInfoRow(stringResource(R.string.shop_owner_business_hours), "${it.opensAt} – ${it.closesAt}")
        }
        review.contactNumber?.let { AdminInfoRow(stringResource(R.string.shop_owner_phone), it) }

        Text(stringResource(R.string.admin_shop_owner_section), style = MaterialTheme.typography.titleMedium)
        AdminInfoRow(
            stringResource(R.string.admin_field_merchant),
            review.owner.fullName?.let { "$it (${review.owner.email})" } ?: review.owner.email,
        )
        review.owner.phone?.let { AdminInfoRow(stringResource(R.string.shop_owner_phone), it) }
        AdminInfoRow(stringResource(R.string.admin_field_owner_id), review.owner.userId)

        Text(stringResource(R.string.admin_shop_verification_section), style = MaterialTheme.typography.titleMedium)
        review.submittedAt?.let {
            AdminInfoRow(stringResource(R.string.admin_field_submitted), DateFormatters.formatShortDate(it))
        }
        AdminInfoRow(stringResource(R.string.shop_owner_approval_status), review.status)
        review.rejectionReason?.let {
            AdminInfoRow(stringResource(R.string.admin_field_previous_rejection), it)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AasPasSpacing.sm)) {
            Button(onClick = onApproveClick, enabled = actionsEnabled, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.admin_shop_approve_action))
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
private fun ShopRejectPresetChips(onSelect: (String) -> Unit) {
    val presets = listOf(
        stringResource(R.string.admin_shop_reject_invalid_info),
        stringResource(R.string.admin_shop_reject_address),
        stringResource(R.string.admin_shop_reject_photo),
        stringResource(R.string.admin_shop_reject_incomplete),
        stringResource(R.string.admin_shop_reject_category),
        stringResource(R.string.admin_shop_reject_unable_verify),
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
