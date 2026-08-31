package com.aaspas.customer.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminOfferReviewViewModel(
    private val repository: AdminRepository,
    private val offerId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminOfferReviewUiState())
    val uiState: StateFlow<AdminOfferReviewUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = AdminOfferReviewStatus.Loading, error = null) }
            when (val result = repository.getOfferReview(offerId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(status = AdminOfferReviewStatus.Loaded, review = result.data)
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(status = AdminOfferReviewStatus.Error, error = result.error)
                    }
                }
            }
        }
    }

    fun showApproveDialog() {
        _uiState.update { it.copy(showApproveDialog = true, validationMessage = null) }
    }

    fun hideApproveDialog() {
        _uiState.update { it.copy(showApproveDialog = false) }
    }

    fun showRejectDialog() {
        _uiState.update { it.copy(showRejectDialog = true, validationMessage = null) }
    }

    fun hideRejectDialog() {
        _uiState.update { it.copy(showRejectDialog = false) }
    }

    fun onRejectReasonChange(value: String) {
        _uiState.update { it.copy(rejectReason = value, validationMessage = null) }
    }

    fun onRejectPresetSelected(value: String) {
        _uiState.update {
            it.copy(selectedRejectPreset = value, rejectReason = value, validationMessage = null)
        }
    }

    fun approveOffer() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(status = AdminOfferReviewStatus.Approving, showApproveDialog = false)
            }
            when (val result = repository.approveOffer(offerId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(status = AdminOfferReviewStatus.Approved) }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(status = AdminOfferReviewStatus.Error, error = result.error)
                    }
                }
            }
        }
    }

    fun rejectOffer() {
        val reason = _uiState.value.rejectReason.trim()
        if (reason.length < 3) {
            _uiState.update { it.copy(validationMessage = "Rejection reason is required (min 3 characters).") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(status = AdminOfferReviewStatus.Rejecting, showRejectDialog = false)
            }
            when (val result = repository.rejectOffer(offerId, reason)) {
                is Result.Success -> {
                    _uiState.update { it.copy(status = AdminOfferReviewStatus.Rejected) }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(status = AdminOfferReviewStatus.Error, error = result.error)
                    }
                }
            }
        }
    }
}
