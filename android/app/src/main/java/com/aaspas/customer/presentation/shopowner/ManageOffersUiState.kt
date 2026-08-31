package com.aaspas.customer.presentation.shopowner

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.MerchantOffer

enum class ManageOffersStatus {
    Loading,
    Loaded,
    Error,
}

data class ManageOffersUiState(
    val status: ManageOffersStatus = ManageOffersStatus.Loading,
    val shopId: String = "",
    val shopName: String = "",
    val offers: List<MerchantOffer> = emptyList(),
    val error: AppError? = null,
)
