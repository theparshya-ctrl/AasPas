package com.aaspas.customer.presentation.shopowner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.MerchantOffer
import com.aaspas.customer.domain.model.MerchantOfferStatus
import com.aaspas.customer.domain.repository.ShopOwnerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ManageOffersViewModel(
    private val repository: ShopOwnerRepository,
    private val shopId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageOffersUiState(shopId = shopId))
    val uiState: StateFlow<ManageOffersUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = ManageOffersStatus.Loading, error = null) }
            when (val dashboard = repository.getDashboard()) {
                is Result.Success -> {
                    _uiState.update { it.copy(shopName = dashboard.data.shop.name) }
                }
                is Result.Failure -> Unit
            }
            when (val result = repository.listOffers(shopId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            status = ManageOffersStatus.Loaded,
                            offers = result.data.sortedWith(offerSortComparator()),
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(status = ManageOffersStatus.Error, error = result.error) }
                }
            }
        }
    }

    private fun offerSortComparator(): Comparator<MerchantOffer> {
        return compareBy<MerchantOffer> { offer -> statusSortOrder(offer.status) }
            .thenByDescending { it.startsAt.orEmpty() }
            .thenBy { it.title.lowercase() }
    }

    private fun statusSortOrder(status: MerchantOfferStatus): Int {
        return when (status) {
            MerchantOfferStatus.Draft -> 0
            MerchantOfferStatus.Rejected -> 1
            MerchantOfferStatus.PendingApproval -> 2
            MerchantOfferStatus.Active -> 3
            MerchantOfferStatus.Scheduled -> 4
            MerchantOfferStatus.Expired -> 5
            MerchantOfferStatus.Unknown -> 6
        }
    }
}
