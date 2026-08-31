package com.aaspas.customer.presentation.home

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.Category
import com.aaspas.customer.domain.model.LocationSource
import com.aaspas.customer.domain.model.Offer
import com.aaspas.customer.domain.model.Shop

enum class SectionStatus {
    Loading,
    Loaded,
    Empty,
    Error,
}

data class SectionUiState<T>(
    val status: SectionStatus = SectionStatus.Loading,
    val items: List<T> = emptyList(),
    val errorType: AppError? = null,
)

data class HomeUiState(
    val isInitialLoad: Boolean = true,
    val isRefreshing: Boolean = false,
    val globalError: String? = null,
    val globalErrorType: AppError? = null,
    val locationLabel: LocationLabelState = LocationLabelState.Loading,
    val locationAvailable: Boolean = false,
    val locationDenied: Boolean = false,
    val localityName: String? = null,
    val locationSource: LocationSource = LocationSource.NONE,
    val categories: SectionUiState<Category> = SectionUiState(),
    val todayOffers: SectionUiState<Offer> = SectionUiState(),
    val comingSoon: SectionUiState<Offer> = SectionUiState(),
    val nearbyShops: SectionUiState<Shop> = SectionUiState(),
    val unreadNotificationCount: Int = 0,
    val showNotificationBell: Boolean = false,
)
