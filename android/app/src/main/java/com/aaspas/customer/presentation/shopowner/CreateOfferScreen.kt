package com.aaspas.customer.presentation.shopowner

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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import com.aaspas.customer.data.mapper.ShopOwnerMapper
import com.aaspas.customer.presentation.components.OfferCard
import com.aaspas.customer.presentation.components.ShopPhotoPickerField
import com.aaspas.customer.presentation.components.PickerDateField
import com.aaspas.customer.presentation.components.PickerTimeField
import com.aaspas.customer.presentation.theme.AasPasColors
import com.aaspas.customer.presentation.theme.AasPasSpacing

@Composable
fun CreateOfferRoute(
    shopId: String,
    shopName: String,
    shopPhotoUrl: String?,
    offerId: String?,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as AasPasApplication
    val viewModel: CreateOfferViewModel = viewModel(
        factory = CreateOfferViewModelFactory(app, app.shopOwnerRepository, shopId, shopName, shopPhotoUrl, offerId),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.status) {
        if (uiState.status == CreateOfferStatus.Submitted) {
            onSubmitted()
        }
    }

    CreateOfferScreen(
        uiState = uiState,
        onBack = {
            if (uiState.step == CreateOfferStep.Preview && !uiState.isReadOnly) {
                viewModel.backToForm()
            } else {
                onBack()
            }
        },
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onDiscountTypeChange = viewModel::onDiscountTypeChange,
        onDiscountValueChange = viewModel::onDiscountValueChange,
        onStartDateChange = viewModel::onStartDateChange,
        onStartTimeChange = viewModel::onStartTimeChange,
        onEndDateChange = viewModel::onEndDateChange,
        onEndTimeChange = viewModel::onEndTimeChange,
        onApplicableProductsChange = viewModel::onApplicableProductsChange,
        onMinPurchaseChange = viewModel::onMinPurchaseChange,
        onTermsChange = viewModel::onTermsChange,
        onPhotoPicked = viewModel::onPhotoPicked,
        onSavePhoto = viewModel::savePendingPhoto,
        onRemovePhoto = viewModel::onPhotoRemoved,
        onMerchantConfirmedChange = viewModel::onMerchantConfirmedChange,
        onSaveDraft = viewModel::saveDraft,
        onPreview = viewModel::showPreview,
        onSubmit = viewModel::submitForVerification,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOfferScreen(
    uiState: CreateOfferUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDiscountTypeChange: (String) -> Unit,
    onDiscountValueChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onApplicableProductsChange: (String) -> Unit,
    onMinPurchaseChange: (String) -> Unit,
    onTermsChange: (String) -> Unit,
    onPhotoPicked: (android.net.Uri) -> Unit,
    onSavePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onMerchantConfirmedChange: (Boolean) -> Unit,
    onSaveDraft: () -> Unit,
    onPreview: () -> Unit,
    onSubmit: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            uiState.isReadOnly -> stringResource(R.string.offer_view_title)
                            uiState.draft.offerId != null -> stringResource(R.string.offer_edit_title)
                            uiState.step == CreateOfferStep.Preview -> stringResource(R.string.offer_preview_title)
                            else -> stringResource(R.string.shop_owner_create_offer)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        containerColor = AasPasColors.Background,
    ) { padding ->
        when {
            uiState.status == CreateOfferStatus.Loading -> {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator(color = AasPasColors.PurplePrimary) }
            }
            uiState.step == CreateOfferStep.Preview -> {
                OfferPreviewStep(
                    uiState = uiState,
                    modifier = Modifier.padding(padding),
                    onMerchantConfirmedChange = onMerchantConfirmedChange,
                    onSubmit = onSubmit,
                    readOnly = uiState.isReadOnly,
                )
            }
            else -> {
                OfferFormStep(
                    uiState = uiState,
                    modifier = Modifier.padding(padding),
                    onTitleChange = onTitleChange,
                    onDescriptionChange = onDescriptionChange,
                    onDiscountTypeChange = onDiscountTypeChange,
                    onDiscountValueChange = onDiscountValueChange,
                    onStartDateChange = onStartDateChange,
                    onStartTimeChange = onStartTimeChange,
                    onEndDateChange = onEndDateChange,
                    onEndTimeChange = onEndTimeChange,
                    onApplicableProductsChange = onApplicableProductsChange,
                    onMinPurchaseChange = onMinPurchaseChange,
                    onTermsChange = onTermsChange,
                    onPhotoPicked = onPhotoPicked,
                    onSavePhoto = onSavePhoto,
                    onRemovePhoto = onRemovePhoto,
                    onSaveDraft = onSaveDraft,
                    onPreview = onPreview,
                )
            }
        }
    }
}

@Composable
private fun OfferFormStep(
    uiState: CreateOfferUiState,
    modifier: Modifier = Modifier,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDiscountTypeChange: (String) -> Unit,
    onDiscountValueChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onApplicableProductsChange: (String) -> Unit,
    onMinPurchaseChange: (String) -> Unit,
    onTermsChange: (String) -> Unit,
    onPhotoPicked: (android.net.Uri) -> Unit,
    onSavePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSaveDraft: () -> Unit,
    onPreview: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AasPasSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.sm),
    ) {
        if (!uiState.canSubmitOffers) {
            Text(
                text = "Your shop must be approved before offers can be submitted for verification.",
                color = AasPasColors.RedCritical,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        uiState.rejectionReason?.let {
            Text(
                text = stringResource(R.string.manage_offer_rejection, it),
                color = AasPasColors.RedCritical,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (uiState.requiresReVerification) {
            Text(
                text = stringResource(R.string.offer_reverification_required),
                color = AasPasColors.PurpleDark,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        OfferField(uiState.draft.title, onTitleChange, stringResource(R.string.offer_field_title))
        OfferField(uiState.draft.description, onDescriptionChange, stringResource(R.string.offer_field_description), singleLine = false)
        Text(stringResource(R.string.offer_field_discount_type), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(AasPasSpacing.sm)) {
            FilterChip(
                selected = uiState.draft.discountType == "percentage",
                onClick = { onDiscountTypeChange("percentage") },
                label = { Text(stringResource(R.string.offer_discount_percentage)) },
            )
            FilterChip(
                selected = uiState.draft.discountType == "fixed",
                onClick = { onDiscountTypeChange("fixed") },
                label = { Text(stringResource(R.string.offer_discount_fixed)) },
            )
        }
        OfferField(uiState.draft.discountValue, onDiscountValueChange, stringResource(R.string.offer_field_discount_value))
        PickerDateField(
            value = uiState.draft.startDate,
            onValueChange = onStartDateChange,
            label = stringResource(R.string.offer_field_start_date),
        )
        PickerTimeField(
            value = uiState.draft.startTime,
            onValueChange = onStartTimeChange,
            label = stringResource(R.string.offer_field_start_time),
        )
        PickerDateField(
            value = uiState.draft.endDate,
            onValueChange = onEndDateChange,
            label = stringResource(R.string.offer_field_end_date),
        )
        PickerTimeField(
            value = uiState.draft.endTime,
            onValueChange = onEndTimeChange,
            label = stringResource(R.string.offer_field_end_time),
        )
        ShopPhotoPickerField(
            photoUrl = uiState.draft.photoUrl,
            localPreviewUri = uiState.pendingPhotoUri?.let { android.net.Uri.parse(it) },
            localPreviewBytes = uiState.pendingPhotoBytes,
            isUploading = uiState.isUploadingPhoto,
            enabled = !uiState.isReadOnly,
            errorMessage = uiState.photoErrorMessage,
            successMessage = if (uiState.photoSaveSucceeded) {
                stringResource(R.string.offer_photo_updated_success)
            } else {
                null
            },
            onPhotoPicked = onPhotoPicked,
            onSavePhoto = onSavePhoto,
            onRemovePhoto = onRemovePhoto,
            fieldTitleRes = R.string.offer_photo,
            previewTitleRes = R.string.offer_photo_preview_title,
            previewDescRes = R.string.offer_photo_preview_desc,
            saveButtonRes = R.string.offer_save_photo,
            chooseGalleryRes = R.string.offer_photo_choose_gallery,
            chooseAnotherRes = R.string.offer_photo_choose_another,
            retakeRes = R.string.offer_photo_retake,
            removeRes = R.string.offer_photo_remove,
            changeRes = R.string.offer_photo_change,
            takeCameraRes = R.string.offer_photo_take_camera,
            uploadingRes = R.string.offer_photo_uploading,
            uploadHintRes = R.string.offer_photo_upload_hint,
            cameraFilePrefix = "offer-photo-",
        )
        OfferField(uiState.draft.applicableProducts, onApplicableProductsChange, stringResource(R.string.offer_field_applicable_products))
        OfferField(uiState.draft.minPurchaseAmount, onMinPurchaseChange, stringResource(R.string.offer_field_min_purchase))
        OfferField(uiState.draft.terms, onTermsChange, stringResource(R.string.offer_field_terms), singleLine = false)
        uiState.validationMessage?.let {
            Text(text = it, color = AasPasColors.RedCritical, style = MaterialTheme.typography.bodyMedium)
        }
        if (uiState.status == CreateOfferStatus.Saved) {
            Text(text = stringResource(R.string.offer_draft_saved), color = AasPasColors.PurpleDark)
        }
        OutlinedButton(onClick = onSaveDraft, enabled = uiState.status != CreateOfferStatus.Saving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.offer_save_draft))
        }
        Button(onClick = onPreview, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.offer_preview_button))
        }
    }
}

@Composable
private fun OfferPreviewStep(
    uiState: CreateOfferUiState,
    modifier: Modifier = Modifier,
    onMerchantConfirmedChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    readOnly: Boolean = false,
) {
    val previewOffer = ShopOwnerMapper.toPreviewOffer(uiState.draft, uiState.shopName, uiState.shopPhotoUrl)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AasPasSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AasPasSpacing.md),
    ) {
        if (!uiState.canSubmitOffers) {
            Text(
                text = "Your shop must be approved before offers can be submitted for verification.",
                color = AasPasColors.RedCritical,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (readOnly) {
            Text(
                text = stringResource(R.string.offer_read_only_hint),
                color = AasPasColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(stringResource(R.string.offer_preview_hint), color = AasPasColors.TextSecondary)
        OfferCard(offer = previewOffer, onClick = {})
        uiState.draft.applicableProducts.takeIf { it.isNotBlank() }?.let {
            Text("${stringResource(R.string.offer_field_applicable_products)}: $it")
        }
        uiState.draft.terms.takeIf { it.isNotBlank() }?.let {
            Text("${stringResource(R.string.offer_field_terms)}: $it")
        }
        Text(
            text = stringResource(
                R.string.offer_preview_validity,
                DateFormatters.formatShortDate(previewOffer.startsAt),
                DateFormatters.formatShortDate(previewOffer.endsAt),
            ),
        )
        Text(stringResource(R.string.offer_confirm_title), style = MaterialTheme.typography.titleMedium)
        if (!readOnly) {
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(checked = uiState.merchantConfirmed, onCheckedChange = onMerchantConfirmedChange)
                Text(
                    text = stringResource(R.string.offer_confirm_text),
                    modifier = Modifier.padding(top = AasPasSpacing.sm),
                )
            }
            uiState.validationMessage?.let {
                Text(text = it, color = AasPasColors.RedCritical)
            }
            Button(
                onClick = onSubmit,
                enabled = uiState.canSubmitOffers && uiState.merchantConfirmed && uiState.status != CreateOfferStatus.Submitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.status == CreateOfferStatus.Submitting) {
                    CircularProgressIndicator(color = AasPasColors.White)
                } else {
                    Text(stringResource(R.string.offer_submit_verification))
                }
            }
        }
    }
}

@Composable
private fun OfferField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
    )
}
