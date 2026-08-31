package com.aaspas.customer.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.repository.CategoryOffersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoryOffersViewModel(
    private val repository: CategoryOffersRepository,
    private val categoryId: String,
    private val categoryName: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CategoryOffersUiState(categoryId = categoryId, categoryName = categoryName),
    )
    val uiState: StateFlow<CategoryOffersUiState> = _uiState.asStateFlow()

    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null

    fun load(latitude: Double?, longitude: Double?) {
        lastLatitude = latitude
        lastLongitude = longitude
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    status = CategoryLoadStatus.Loading,
                    error = null,
                    locationAvailable = latitude != null && longitude != null,
                )
            }
            when (
                val result = repository.getCategoryOffers(
                    categoryId = categoryId,
                    latitude = latitude,
                    longitude = longitude,
                )
            ) {
                is Result.Success -> {
                    val feed = result.data
                    val hasOffers = feed.todayOffers.isNotEmpty() || feed.comingSoon.isNotEmpty()
                    _uiState.update {
                        it.copy(
                            status = if (hasOffers) CategoryLoadStatus.Loaded else CategoryLoadStatus.Empty,
                            categoryName = feed.categoryName.ifBlank { categoryName },
                            todayOffers = feed.todayOffers,
                            comingSoon = feed.comingSoon,
                            totalActive = feed.totalActive,
                            error = null,
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            status = CategoryLoadStatus.Error,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    fun retry() {
        load(lastLatitude, lastLongitude)
    }
}
