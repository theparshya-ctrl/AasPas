package com.aaspas.customer.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.repository.AuthRepository
import com.aaspas.customer.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null

    fun load(latitude: Double? = null, longitude: Double? = null) {
        lastLatitude = latitude
        lastLongitude = longitude
        if (!authRepository.isLoggedIn()) {
            _uiState.update { it.copy(status = FavoritesLoadStatus.Unauthorized, error = AppError.Unauthorized) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(status = FavoritesLoadStatus.Loading, error = null) }
            when (val result = favoritesRepository.listFavorites(latitude, longitude)) {
                is Result.Success -> {
                    val feed = result.data
                    val isEmpty = feed.offers.isEmpty() && feed.shops.isEmpty()
                    _uiState.update {
                        it.copy(
                            status = if (isEmpty) FavoritesLoadStatus.Empty else FavoritesLoadStatus.Loaded,
                            offers = feed.offers,
                            shops = feed.shops,
                            error = null,
                        )
                    }
                }
                is Result.Failure -> {
                    val status = if (result.error is AppError.Unauthorized) {
                        FavoritesLoadStatus.Unauthorized
                    } else {
                        FavoritesLoadStatus.Error
                    }
                    _uiState.update { it.copy(status = status, error = result.error) }
                }
            }
        }
    }

    fun retry() = load(lastLatitude, lastLongitude)

    fun unsaveOffer(offerId: String) {
        viewModelScope.launch {
            when (favoritesRepository.unsaveOffer(offerId)) {
                is Result.Success -> load(lastLatitude, lastLongitude)
                is Result.Failure -> _uiState.update { it.copy(error = AppError.Server) }
            }
        }
    }

    fun unsaveShop(shopId: String) {
        viewModelScope.launch {
            when (favoritesRepository.unsaveShop(shopId)) {
                is Result.Success -> load(lastLatitude, lastLongitude)
                is Result.Failure -> _uiState.update { it.copy(error = AppError.Server) }
            }
        }
    }
}
