package com.aaspas.customer.presentation.offerdetails

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.OfferDetails

enum class DetailsLoadStatus {
    Loading,
    Loaded,
    NotFound,
    Error,
}

data class OfferDetailsUiState(
    val status: DetailsLoadStatus = DetailsLoadStatus.Loading,
    val offer: OfferDetails? = null,
    val error: AppError? = null,
)
