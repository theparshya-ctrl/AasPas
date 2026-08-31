package com.aaspas.customer.presentation.shopowner

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.MerchantOfferDraft
import com.aaspas.customer.domain.model.MerchantOfferStatus

enum class CreateOfferStep {
    Form,
    Preview,
}

enum class CreateOfferStatus {
    Idle,
    Loading,
    Saving,
    Submitting,
    Saved,
    Submitted,
    Error,
}

data class CreateOfferUiState(
    val step: CreateOfferStep = CreateOfferStep.Form,
    val status: CreateOfferStatus = CreateOfferStatus.Idle,
    val shopId: String = "",
    val shopName: String = "",
    val shopPhotoUrl: String? = null,
    val canSubmitOffers: Boolean = false,
    val draft: MerchantOfferDraft = MerchantOfferDraft(shopId = ""),
    val loadedOfferStatus: MerchantOfferStatus? = null,
    val isReadOnly: Boolean = false,
    val rejectionReason: String? = null,
    val requiresReVerification: Boolean = false,
    val merchantConfirmed: Boolean = false,
    val validationMessage: String? = null,
    val error: AppError? = null,
    val pendingPhotoUri: String? = null,
    val pendingPhotoBytes: ByteArray? = null,
    val isUploadingPhoto: Boolean = false,
    val photoErrorMessage: String? = null,
    val photoSaveSucceeded: Boolean = false,
)
