package com.aaspas.customer.presentation.shopowner

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.MerchantBusinessHours
import com.aaspas.customer.domain.model.MerchantShopLocationDraft
import com.aaspas.customer.domain.model.MerchantShopProfile

enum class ShopOwnerProfileLoadStatus {
    Loading,
    Loaded,
    Saving,
    Saved,
    Error,
    Unauthorized,
}

data class ShopOwnerProfileUiState(
    val status: ShopOwnerProfileLoadStatus = ShopOwnerProfileLoadStatus.Loading,
    val shopId: String = "",
    val canEdit: Boolean = false,
    val canEditPhoto: Boolean = false,
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val contactNumber: String = "",
    val photoUrl: String = "",
    val pendingPhotoUri: String? = null,
    val pendingPhotoBytes: ByteArray? = null,
    val isUploadingPhoto: Boolean = false,
    val photoErrorMessage: String? = null,
    val photoSaveSucceeded: Boolean = false,
    val opensAt: String = "",
    val closesAt: String = "",
    val locationDraft: MerchantShopLocationDraft = MerchantShopLocationDraft(),
    val shopStatus: String = "",
    val statusMessage: String? = null,
    val error: AppError? = null,
    val validationMessage: String? = null,
    val shouldNavigateBack: Boolean = false,
)
