package com.aaspas.customer.presentation.search

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.domain.model.Category
import com.aaspas.customer.domain.model.Offer

enum class SearchLoadStatus {
    Idle,
    Loading,
    Loaded,
    Empty,
    Error,
}

data class SearchUiState(
    val query: String = "",
    val selectedCategoryId: String? = null,
    val categories: List<Category> = emptyList(),
    val status: SearchLoadStatus = SearchLoadStatus.Idle,
    val offers: List<Offer> = emptyList(),
    val total: Int = 0,
    val error: AppError? = null,
    val locationAvailable: Boolean = false,
)
