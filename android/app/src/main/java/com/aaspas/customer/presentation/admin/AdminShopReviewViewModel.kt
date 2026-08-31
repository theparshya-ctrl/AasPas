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

class AdminShopReviewViewModel(
    private val repository: AdminRepository,
    private val shopId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminShopReviewUiState())
    val uiState: StateFlow<AdminShopReviewUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = AdminShopReviewStatus.Loading, error = null) }
            when (val result = repository.getShopReview(shopId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(status = AdminShopReviewStatus.Loaded, review = result.data)
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(status = AdminShopReviewStatus.Error, error = result.error)
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

    fun approveShop() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(status = AdminShopReviewStatus.Approving, showApproveDialog = false)
            }
            when (val result = repository.approveShop(shopId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(status = AdminShopReviewStatus.Approved) }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(status = AdminShopReviewStatus.Error, error = result.error)
                    }
                }
            }
        }
    }

    fun rejectShop() {
        val reason = _uiState.value.rejectReason.trim()
        if (reason.length < 3) {
            _uiState.update { it.copy(validationMessage = "Rejection reason is required (min 3 characters).") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(status = AdminShopReviewStatus.Rejecting, showRejectDialog = false)
            }
            when (val result = repository.rejectShop(shopId, reason)) {
                is Result.Success -> {
                    _uiState.update { it.copy(status = AdminShopReviewStatus.Rejected) }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(status = AdminShopReviewStatus.Error, error = result.error)
                    }
                }
            }
        }
    }
}
