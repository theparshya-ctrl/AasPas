package com.aaspas.customer.presentation.shopdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.repository.ShopDetailsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShopDetailsViewModel(
    private val repository: ShopDetailsRepository,
    private val shopId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopDetailsUiState())
    val uiState: StateFlow<ShopDetailsUiState> = _uiState.asStateFlow()

    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null
    private var hasLoaded = false

    fun load(latitude: Double? = null, longitude: Double? = null) {
        if (shopId.isBlank()) {
            _uiState.update {
                it.copy(status = DetailsLoadStatus.NotFound, shop = null, error = AppError.NotFound)
            }
            return
        }

        lastLatitude = latitude
        lastLongitude = longitude

        viewModelScope.launch {
            _uiState.update { it.copy(status = DetailsLoadStatus.Loading, error = null) }
            when (val result = repository.getShopDetails(shopId, latitude, longitude)) {
                is Result.Success -> {
                    hasLoaded = true
                    _uiState.update {
                        it.copy(
                            status = DetailsLoadStatus.Loaded,
                            shop = result.data,
                            error = null,
                        )
                    }
                }
                is Result.Failure -> applyFailure(result.error)
            }
        }
    }

    fun retry() {
        load(lastLatitude, lastLongitude)
    }

    fun tryBeginInitialLoad(): Boolean {
        if (hasLoaded) return false
        return true
    }

    private fun applyFailure(error: AppError) {
        hasLoaded = true
        _uiState.update {
            it.copy(
                status = if (error is AppError.NotFound) {
                    DetailsLoadStatus.NotFound
                } else {
                    DetailsLoadStatus.Error
                },
                shop = null,
                error = error,
            )
        }
    }
}
