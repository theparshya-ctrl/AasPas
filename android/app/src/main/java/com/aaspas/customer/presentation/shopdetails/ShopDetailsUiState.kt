package com.aaspas.customer.presentation.shopdetails

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.ShopDetails

enum class DetailsLoadStatus {
    Loading,
    Loaded,
    NotFound,
    Error,
}

data class ShopDetailsUiState(
    val status: DetailsLoadStatus = DetailsLoadStatus.Loading,
    val shop: ShopDetails? = null,
    val error: AppError? = null,
)
