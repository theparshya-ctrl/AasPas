package com.aaspas.customer.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.HomeFeed
import com.aaspas.customer.domain.model.Offer
import com.aaspas.customer.domain.model.Shop
import com.aaspas.customer.domain.model.LocationSource
import com.aaspas.customer.core.notifications.RealtimeNotificationStore
import com.aaspas.customer.core.startup.StartupTracer
import com.aaspas.customer.domain.repository.AuthRepository
import com.aaspas.customer.domain.repository.FavoritesRepository
import com.aaspas.customer.domain.repository.HomeRepository
import com.aaspas.customer.domain.repository.NotificationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: HomeRepository,
    private val favoritesRepository: FavoritesRepository,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val realtimeNotificationStore: RealtimeNotificationStore,
) : ViewModel() {

    sealed class SaveToggleOutcome {
        data object NeedLogin : SaveToggleOutcome()
        data object Success : SaveToggleOutcome()
        data class Error(val error: AppError) : SaveToggleOutcome()
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null
    private var lastLocationDenied: Boolean = false
    private var loadJob: Job? = null
    private var hasLoadedInitial = false
    private var initialLoadStarted = false

    init {
        viewModelScope.launch {
            realtimeNotificationStore.unreadCount.collect { count ->
                _uiState.update {
                    it.copy(
                        unreadNotificationCount = count,
                        showNotificationBell = authRepository.isLoggedIn(),
                    )
                }
            }
        }
    }

    private var lastLocationSource: LocationSource = LocationSource.NONE
    private var lastDisplayName: String? = null

    fun loadHome(
        latitude: Double?,
        longitude: Double?,
        locationDenied: Boolean = false,
        isRefresh: Boolean = false,
        locationSource: LocationSource = lastLocationSource,
        displayName: String? = lastDisplayName,
    ) {
        if (!isRefresh && loadJob?.isActive == true) return

        lastLatitude = latitude
        lastLongitude = longitude
        lastLocationDenied = locationDenied
        lastLocationSource = locationSource
        lastDisplayName = displayName?.trim()?.takeIf { it.isNotEmpty() }

        loadJob = viewModelScope.launch {
            val locationLabel = resolveLocationLabel(
                locationDenied = locationDenied,
                latitude = latitude,
                longitude = longitude,
                locationSource = locationSource,
                displayName = lastDisplayName,
                preserveCurrent = isRefresh,
            )

            _uiState.update { current ->
                current.copy(
                    isInitialLoad = !isRefresh && !hasLoadedInitial,
                    isRefreshing = true,
                    globalError = null,
                    globalErrorType = null,
                    locationDenied = locationDenied,
                    locationLabel = locationLabel,
                    locationSource = locationSource,
                    localityName = lastDisplayName,
                    categories = if (isRefresh) current.categories else SectionUiState(),
                    todayOffers = if (isRefresh) current.todayOffers else SectionUiState(),
                    comingSoon = if (isRefresh) current.comingSoon else SectionUiState(),
                    nearbyShops = if (isRefresh) current.nearbyShops else SectionUiState(),
                )
            }

            when (val result = repository.getHome(latitude, longitude)) {
                is Result.Success -> applySuccess(
                    feed = result.data,
                    locationDenied = locationDenied,
                    locationSource = locationSource,
                    displayName = lastDisplayName,
                )
                is Result.Failure -> applyFailure(result.error)
            }
        }
    }

    fun retry() {
        loadHome(
            latitude = lastLatitude,
            longitude = lastLongitude,
            locationDenied = lastLocationDenied,
            isRefresh = true,
            locationSource = lastLocationSource,
            displayName = lastDisplayName,
        )
    }

    fun toggleOfferSave(offerId: String, onOutcome: (SaveToggleOutcome) -> Unit = {}) {
        if (!authRepository.isLoggedIn()) {
            onOutcome(SaveToggleOutcome.NeedLogin)
            return
        }
        viewModelScope.launch {
            val currentlySaved = findOffer(offerId)?.isSaved == true
            val result = if (currentlySaved) {
                favoritesRepository.unsaveOffer(offerId)
            } else {
                favoritesRepository.saveOffer(offerId)
            }
            when (result) {
                is Result.Success -> {
                    patchOfferSaved(offerId, result.data)
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

    fun toggleShopSave(shopId: String, onOutcome: (SaveToggleOutcome) -> Unit = {}) {
        if (!authRepository.isLoggedIn()) {
            onOutcome(SaveToggleOutcome.NeedLogin)
            return
        }
        viewModelScope.launch {
            val currentlySaved = findShop(shopId)?.isSaved == true
            val result = if (currentlySaved) {
                favoritesRepository.unsaveShop(shopId)
            } else {
                favoritesRepository.saveShop(shopId)
            }
            when (result) {
                is Result.Success -> {
                    patchShopSaved(shopId, result.data)
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

    private fun findOffer(offerId: String): Offer? {
        val state = _uiState.value
        return state.todayOffers.items.find { it.id == offerId }
            ?: state.comingSoon.items.find { it.id == offerId }
    }

    private fun findShop(shopId: String): Shop? {
        return _uiState.value.nearbyShops.items.find { it.id == shopId }
    }

    private fun patchOfferSaved(offerId: String, isSaved: Boolean) {
        _uiState.update { state ->
            state.copy(
                todayOffers = state.todayOffers.copy(
                    items = state.todayOffers.items.map { if (it.id == offerId) it.copy(isSaved = isSaved) else it },
                ),
                comingSoon = state.comingSoon.copy(
                    items = state.comingSoon.items.map { if (it.id == offerId) it.copy(isSaved = isSaved) else it },
                ),
            )
        }
    }

    private fun patchShopSaved(shopId: String, isSaved: Boolean) {
        _uiState.update { state ->
            state.copy(
                nearbyShops = state.nearbyShops.copy(
                    items = state.nearbyShops.items.map { if (it.id == shopId) it.copy(isSaved = isSaved) else it },
                ),
            )
        }
    }

    fun setLocalityName(name: String?, source: LocationSource = lastLocationSource) {
        _uiState.update {
            it.copy(
                localityName = name?.trim()?.takeIf { n -> n.isNotEmpty() },
                locationLabel = if (!name.isNullOrBlank()) LocationLabelState.Locality else it.locationLabel,
                locationSource = source,
            )
        }
        lastDisplayName = name?.trim()?.takeIf { it.isNotEmpty() }
        lastLocationSource = source
    }

    fun applySelectedLocation(
        latitude: Double?,
        longitude: Double?,
        displayName: String?,
        source: LocationSource,
        locationDenied: Boolean = false,
        isRefresh: Boolean = true,
    ) {
        loadHome(
            latitude = latitude,
            longitude = longitude,
            locationDenied = locationDenied,
            isRefresh = isRefresh,
            locationSource = source,
            displayName = displayName,
        )
    }

    fun tryBeginInitialLoad(): Boolean {
        if (initialLoadStarted) return false
        initialLoadStarted = true
        return true
    }

    fun onSessionChanged(isLoggedIn: Boolean) {
        if (isLoggedIn) {
            retry()
        } else {
            _uiState.update { state ->
                state.copy(
                    todayOffers = state.todayOffers.copy(
                        items = state.todayOffers.items.map { it.copy(isSaved = false) },
                    ),
                    comingSoon = state.comingSoon.copy(
                        items = state.comingSoon.items.map { it.copy(isSaved = false) },
                    ),
                    nearbyShops = state.nearbyShops.copy(
                        items = state.nearbyShops.items.map { it.copy(isSaved = false) },
                    ),
                )
            }
        }
    }

    private fun applySuccess(
        feed: HomeFeed,
        locationDenied: Boolean,
        locationSource: LocationSource,
        displayName: String?,
    ) {
        val firstLoad = !hasLoadedInitial
        hasLoadedInitial = true
        if (firstLoad) {
            StartupTracer.mark("home_api_success")
        }
        val resolvedLabel = when {
            !displayName.isNullOrBlank() -> LocationLabelState.Locality
            locationSource == LocationSource.CURRENT_GPS && feed.location.available -> LocationLabelState.NearYou
            latitudeAvailable(feed, locationDenied) -> LocationLabelState.NearYou
            locationDenied -> LocationLabelState.Unavailable
            else -> LocationLabelState.Unavailable
        }
        _uiState.update {
            it.copy(
                isInitialLoad = false,
                isRefreshing = false,
                globalError = null,
                globalErrorType = null,
                locationAvailable = feed.location.available || locationSource == LocationSource.MANUAL,
                locationDenied = locationDenied,
                locationLabel = resolvedLabel,
                locationSource = locationSource,
                localityName = displayName ?: it.localityName,
                categories = feed.categories.toSection(),
                todayOffers = feed.todayOffers.toSection(),
                comingSoon = feed.comingSoon.toSection(),
                nearbyShops = feed.nearbyShops.toSection(),
            )
        }
    }

    private fun latitudeAvailable(feed: HomeFeed, locationDenied: Boolean): Boolean {
        return !locationDenied && feed.location.available
    }

    private fun applyFailure(error: AppError) {
        hasLoadedInitial = true
        _uiState.update {
            val resolvedLabel = when {
                it.locationDenied -> LocationLabelState.Unavailable
                it.locationLabel == LocationLabelState.Loading -> LocationLabelState.Unavailable
                else -> it.locationLabel
            }
            it.copy(
                isInitialLoad = false,
                isRefreshing = false,
                globalError = error.message,
                globalErrorType = error,
                locationLabel = resolvedLabel,
                categories = it.categories.withError(error),
                todayOffers = it.todayOffers.withError(error),
                comingSoon = it.comingSoon.withError(error),
                nearbyShops = it.nearbyShops.withError(error),
            )
        }
    }

    private fun resolveLocationLabel(
        locationDenied: Boolean,
        latitude: Double?,
        longitude: Double?,
        locationSource: LocationSource,
        displayName: String?,
        preserveCurrent: Boolean,
    ): LocationLabelState {
        if (preserveCurrent) {
            val current = _uiState.value.locationLabel
            if (current != LocationLabelState.Loading) return current
        }
        return when {
            !displayName.isNullOrBlank() -> LocationLabelState.Locality
            locationDenied -> LocationLabelState.Unavailable
            latitude != null && longitude != null -> {
                if (locationSource == LocationSource.CURRENT_GPS) LocationLabelState.NearYou else LocationLabelState.Locality
            }
            else -> LocationLabelState.Loading
        }
    }

    private fun <T> List<T>.toSection(): SectionUiState<T> {
        return if (isEmpty()) {
            SectionUiState(status = SectionStatus.Empty)
        } else {
            SectionUiState(status = SectionStatus.Loaded, items = this)
        }
    }

    private fun <T> SectionUiState<T>.withError(error: AppError): SectionUiState<T> {
        return if (status == SectionStatus.Loading || items.isEmpty()) {
            copy(status = SectionStatus.Error, errorType = error)
        } else {
            this
        }
    }

    fun refreshNotifications() {
        if (!authRepository.isLoggedIn()) {
            _uiState.update { it.copy(unreadNotificationCount = 0, showNotificationBell = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(showNotificationBell = true) }
            when (val result = notificationRepository.listNotifications(unreadOnly = false)) {
                is Result.Success -> {
                    realtimeNotificationStore.applyFromRest(result.data)
                    _uiState.update {
                        it.copy(
                            unreadNotificationCount = result.data.unreadCount,
                            showNotificationBell = true,
                        )
                    }
                }
                is Result.Failure -> Unit
            }
        }
    }

    fun onLoginStateChanged(isLoggedIn: Boolean) {
        if (isLoggedIn) {
            refreshNotifications()
        } else {
            _uiState.update { it.copy(unreadNotificationCount = 0, showNotificationBell = false) }
        }
    }
}
