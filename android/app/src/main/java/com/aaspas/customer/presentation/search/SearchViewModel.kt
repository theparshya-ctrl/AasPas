package com.aaspas.customer.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.repository.AuthRepository
import com.aaspas.customer.domain.repository.FavoritesRepository
import com.aaspas.customer.domain.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repository: SearchRepository,
    private val favoritesRepository: FavoritesRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    sealed class SaveToggleOutcome {
        data object NeedLogin : SaveToggleOutcome()
        data object Success : SaveToggleOutcome()
        data class Error(val error: AppError) : SaveToggleOutcome()
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null

    companion object {
        private const val DEBOUNCE_MS = 400L
        private const val MIN_QUERY_LENGTH = 2
    }

    fun loadCategories() {
        viewModelScope.launch {
            when (val result = repository.listCategories()) {
                is Result.Success -> _uiState.update { it.copy(categories = result.data) }
                is Result.Failure -> Unit
            }
        }
    }

    fun setLocation(latitude: Double?, longitude: Double?) {
        val locationChanged = lastLatitude != latitude || lastLongitude != longitude
        lastLatitude = latitude
        lastLongitude = longitude
        _uiState.update {
            it.copy(locationAvailable = latitude != null && longitude != null)
        }
        if (!locationChanged) return
        val query = _uiState.value.query.trim()
        val categoryId = _uiState.value.selectedCategoryId
        if (query.length >= MIN_QUERY_LENGTH || categoryId != null) {
            performSearchImmediate()
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        scheduleSearch()
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.update {
            it.copy(selectedCategoryId = if (it.selectedCategoryId == categoryId) null else categoryId)
        }
        performSearchImmediate()
    }

    fun retry() {
        performSearchImmediate()
    }

    fun clearQuery() {
        _uiState.update { it.copy(query = "") }
        scheduleSearch()
    }

    fun onSessionChanged(isLoggedIn: Boolean) {
        if (isLoggedIn) {
            retry()
        } else {
            _uiState.update { state ->
                state.copy(offers = state.offers.map { it.copy(isSaved = false) })
            }
        }
    }

    fun toggleOfferSave(offerId: String, onOutcome: (SaveToggleOutcome) -> Unit = {}) {
        if (!authRepository.isLoggedIn()) {
            onOutcome(SaveToggleOutcome.NeedLogin)
            return
        }
        viewModelScope.launch {
            val currentlySaved = _uiState.value.offers.find { it.id == offerId }?.isSaved == true
            val result = if (currentlySaved) {
                favoritesRepository.unsaveOffer(offerId)
            } else {
                favoritesRepository.saveOffer(offerId)
            }
            when (result) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            offers = state.offers.map { offer ->
                                if (offer.id == offerId) offer.copy(isSaved = result.data) else offer
                            },
                        )
                    }
                    onOutcome(SaveToggleOutcome.Success)
                }
                is Result.Failure -> {
                    val outcome = if (result.error is AppError.Unauthorized) {
                        SaveToggleOutcome.NeedLogin
                    } else {
                        SaveToggleOutcome.Error(result.error)
                    }
                    onOutcome(outcome)
                }
            }
        }
    }

    private fun scheduleSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            val query = _uiState.value.query.trim()
            val categoryId = _uiState.value.selectedCategoryId
            if (query.length < MIN_QUERY_LENGTH && categoryId == null) {
                _uiState.update {
                    it.copy(status = SearchLoadStatus.Idle, offers = emptyList(), total = 0, error = null)
                }
                return@launch
            }
            executeSearch(query.takeIf { it.length >= MIN_QUERY_LENGTH }, categoryId)
        }
    }

    private fun performSearchImmediate() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val query = _uiState.value.query.trim()
            val categoryId = _uiState.value.selectedCategoryId
            if (query.length < MIN_QUERY_LENGTH && categoryId == null) {
                _uiState.update {
                    it.copy(status = SearchLoadStatus.Idle, offers = emptyList(), total = 0, error = null)
                }
                return@launch
            }
            executeSearch(query.takeIf { it.length >= MIN_QUERY_LENGTH }, categoryId)
        }
    }

    private suspend fun executeSearch(query: String?, categoryId: String?) {
        _uiState.update { it.copy(status = SearchLoadStatus.Loading, error = null) }
        when (
            val result = repository.search(
                query = query,
                categoryId = categoryId,
                latitude = lastLatitude,
                longitude = lastLongitude,
            )
        ) {
            is Result.Success -> {
                val offers = result.data.offers
                _uiState.update {
                    it.copy(
                        status = if (offers.isEmpty()) SearchLoadStatus.Empty else SearchLoadStatus.Loaded,
                        offers = offers,
                        total = result.data.total,
                        error = null,
                    )
                }
            }
            is Result.Failure -> {
                _uiState.update {
                    it.copy(
                        status = SearchLoadStatus.Error,
                        offers = emptyList(),
                        error = result.error,
                    )
                }
            }
        }
    }
}
