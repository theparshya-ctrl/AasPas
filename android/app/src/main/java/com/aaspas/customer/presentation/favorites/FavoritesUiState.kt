package com.aaspas.customer.presentation.favorites

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.FavoriteOffer
import com.aaspas.customer.domain.model.FavoriteShop

enum class FavoritesLoadStatus {
    Loading,
    Loaded,
    Empty,
    Error,
    Unauthorized,
}

data class FavoritesUiState(
    val status: FavoritesLoadStatus = FavoritesLoadStatus.Loading,
    val offers: List<FavoriteOffer> = emptyList(),
    val shops: List<FavoriteShop> = emptyList(),
    val error: AppError? = null,
)
