package com.aaspas.customer.presentation.shopowner



import com.aaspas.customer.core.common.AppError

import com.aaspas.customer.domain.model.MerchantShopLocationDraft



enum class ShopOwnerOnboardingStatus {

    Ready,

    Submitting,

    Success,

    Error,

}



data class ShopOwnerOnboardingUiState(

    val status: ShopOwnerOnboardingStatus = ShopOwnerOnboardingStatus.Ready,

    val name: String = "",

    val category: String = "",

    val description: String = "",

    val contactNumber: String = "",

    val photoUrl: String = "",
    val pendingPhotoUri: String? = null,
    val pendingPhotoBytes: ByteArray? = null,
    val isUploadingPhoto: Boolean = false,
    val photoErrorMessage: String? = null,

    val opensAt: String = "09:00",

    val closesAt: String = "21:00",

    val locationDraft: MerchantShopLocationDraft = MerchantShopLocationDraft(),

    val validationMessage: String? = null,

    val error: AppError? = null,

    val createdShopId: String? = null,

)


