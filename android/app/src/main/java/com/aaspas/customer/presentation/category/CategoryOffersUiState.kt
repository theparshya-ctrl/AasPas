package com.aaspas.customer.presentation.category

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.Offer

enum class CategoryLoadStatus {
    Loading,
    Loaded,
    Empty,
    Error,
}

data class CategoryOffersUiState(
    val categoryId: String = "",
    val categoryName: String = "",
    val status: CategoryLoadStatus = CategoryLoadStatus.Loading,
    val todayOffers: List<Offer> = emptyList(),
    val comingSoon: List<Offer> = emptyList(),
    val totalActive: Int = 0,
    val error: AppError? = null,
    val locationAvailable: Boolean = false,
)
