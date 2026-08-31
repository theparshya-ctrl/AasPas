package com.aaspas.customer.presentation.shopowner



import android.app.Application

import android.net.Uri

import androidx.lifecycle.AndroidViewModel

import androidx.lifecycle.viewModelScope

import com.aaspas.customer.R

import com.aaspas.customer.core.common.AppError

import com.aaspas.customer.core.common.Result

import com.aaspas.customer.core.common.userMessage

import com.aaspas.customer.core.media.ImageCompressor

import com.aaspas.customer.data.mapper.ShopOwnerMapper

import com.aaspas.customer.domain.model.MerchantOfferDraft

import com.aaspas.customer.domain.model.MerchantOfferStatus

import com.aaspas.customer.domain.repository.ShopOwnerRepository

import com.aaspas.customer.domain.shopowner.MerchantOfferValidator

import kotlinx.coroutines.CoroutineDispatcher

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.update

import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext



class CreateOfferViewModel(

    application: Application,

    private val repository: ShopOwnerRepository,

    shopId: String,

    shopName: String,

    shopPhotoUrl: String?,

    offerId: String?,

    private val compressImage: (Application, Uri) -> ByteArray? = { app, uri ->

        ImageCompressor.compressImage(app, uri)

    },

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,

) : AndroidViewModel(application) {



    companion object {

        private const val COMPRESS_FAILED_FALLBACK =

            "Could not read the selected photo. Try another image or retake."

    }



    private val _uiState = MutableStateFlow(

        CreateOfferUiState(

            shopId = shopId,

            shopName = shopName,

            shopPhotoUrl = shopPhotoUrl,

            draft = MerchantOfferDraft(shopId = shopId, offerId = offerId),

        ),

    )

    val uiState: StateFlow<CreateOfferUiState> = _uiState.asStateFlow()



    init {

        viewModelScope.launch {

            when (val result = repository.getDashboard()) {

                is Result.Success -> {

                    _uiState.update {

                        it.copy(

                            shopName = if (shopName.isBlank()) result.data.shop.name else shopName,

                            shopPhotoUrl = shopPhotoUrl ?: result.data.shop.photoUrl,

                            canSubmitOffers = result.data.canSubmitOffers,

                        )

                    }

                }

                is Result.Failure -> {

                    if (shopName.isBlank()) Unit else {

                        _uiState.update {

                            it.copy(shopName = shopName, shopPhotoUrl = shopPhotoUrl)

                        }

                    }

                }

            }

        }

        if (!offerId.isNullOrBlank()) {

            loadOffer(offerId)

        }

    }



    private fun loadOffer(offerId: String) {

        viewModelScope.launch {

            _uiState.update { it.copy(status = CreateOfferStatus.Loading) }

            when (val result = repository.getOffer(offerId)) {

                is Result.Success -> {

                    val offer = result.data

                    val editable = MerchantOfferValidator.isEditable(offer.status)

                    _uiState.update {

                        it.copy(

                            status = CreateOfferStatus.Idle,

                            draft = ShopOwnerMapper.draftFromOffer(offer),

                            loadedOfferStatus = offer.status,

                            isReadOnly = !editable,

                            rejectionReason = offer.rejectionReason,

                            requiresReVerification = offer.status == MerchantOfferStatus.Rejected ||

                                offer.merchantConfirmedAt == null,

                            merchantConfirmed = offer.merchantConfirmedAt != null,

                            step = if (!editable) CreateOfferStep.Preview else CreateOfferStep.Form,

                        )

                    }

                }

                is Result.Failure -> {

                    _uiState.update { it.copy(status = CreateOfferStatus.Error, error = result.error) }

                }

            }

        }

    }



    fun onTitleChange(value: String) = updateDraft { it.copy(title = value) }

    fun onDescriptionChange(value: String) = updateDraft { it.copy(description = value) }

    fun onDiscountTypeChange(value: String) = updateDraft { it.copy(discountType = value) }

    fun onDiscountValueChange(value: String) = updateDraft { it.copy(discountValue = value) }

    fun onStartDateChange(value: String) = updateDraft { it.copy(startDate = value) }

    fun onStartTimeChange(value: String) = updateDraft { it.copy(startTime = value) }

    fun onEndDateChange(value: String) = updateDraft { it.copy(endDate = value) }

    fun onEndTimeChange(value: String) = updateDraft { it.copy(endTime = value) }

    fun onApplicableProductsChange(value: String) = updateDraft { it.copy(applicableProducts = value) }

    fun onMinPurchaseChange(value: String) = updateDraft { it.copy(minPurchaseAmount = value) }

    fun onTermsChange(value: String) = updateDraft { it.copy(terms = value) }

    fun onMerchantConfirmedChange(value: Boolean) {

        _uiState.update { it.copy(merchantConfirmed = value, validationMessage = null) }

    }



    fun onPhotoPicked(uri: Uri) {

        if (_uiState.value.isReadOnly) return

        _uiState.update {

            it.copy(

                pendingPhotoUri = uri.toString(),

                pendingPhotoBytes = null,

                photoErrorMessage = null,

                photoSaveSucceeded = false,

            )

        }

    }



    fun onPhotoRemoved() {

        if (_uiState.value.isReadOnly) return

        _uiState.update {

            it.copy(

                pendingPhotoUri = null,

                pendingPhotoBytes = null,

                photoErrorMessage = null,

                photoSaveSucceeded = false,

            )

        }

    }



    fun savePendingPhoto() {

        if (_uiState.value.isReadOnly) return

        if (_uiState.value.pendingPhotoUri == null && _uiState.value.pendingPhotoBytes == null) return

        viewModelScope.launch {

            uploadPendingPhoto(showErrors = true)

        }

    }



    fun showPreview() {

        if (_uiState.value.isReadOnly) return

        val message = MerchantOfferValidator.validateForSubmit(_uiState.value.draft)

        if (message != null) {

            _uiState.update { it.copy(validationMessage = message) }

            return

        }

        viewModelScope.launch {

            if (!uploadPendingPhoto(showErrors = true)) return@launch

            _uiState.update { it.copy(step = CreateOfferStep.Preview, validationMessage = null) }

        }

    }



    fun backToForm() {

        _uiState.update { it.copy(step = CreateOfferStep.Form) }

    }



    fun saveDraft() {

        if (_uiState.value.isReadOnly) return

        val draft = _uiState.value.draft

        val message = MerchantOfferValidator.validateDraft(draft)

        if (message != null) {

            _uiState.update { it.copy(validationMessage = message) }

            return

        }

        viewModelScope.launch {

            _uiState.update { it.copy(status = CreateOfferStatus.Saving, validationMessage = null) }

            val result = if (draft.offerId.isNullOrBlank()) {

                repository.createOffer(draft)

            } else {

                repository.updateOffer(draft)

            }

            when (result) {

                is Result.Success -> {

                    val offer = result.data

                    _uiState.update {

                        it.copy(

                            status = CreateOfferStatus.Saved,

                            draft = ShopOwnerMapper.draftFromOffer(offer),

                            loadedOfferStatus = offer.status,

                            requiresReVerification = offer.merchantConfirmedAt == null,

                            merchantConfirmed = offer.merchantConfirmedAt != null,

                        )

                    }

                }

                is Result.Failure -> {

                    _uiState.update {

                        it.copy(

                            status = CreateOfferStatus.Idle,

                            error = result.error,

                            validationMessage = result.error.userMessage(),

                        )

                    }

                }

            }

        }

    }



    fun submitForVerification() {

        val state = _uiState.value

        if (state.isReadOnly) return

        if (!state.canSubmitOffers) {

            _uiState.update {

                it.copy(

                    validationMessage = "Your shop must be approved before this offer can be submitted for verification.",

                )

            }

            return

        }

        if (!state.merchantConfirmed) {

            _uiState.update { it.copy(validationMessage = "You must confirm this offer before submitting.") }

            return

        }

        val message = MerchantOfferValidator.validateForSubmit(state.draft)

        if (message != null) {

            _uiState.update { it.copy(validationMessage = message) }

            return

        }

        viewModelScope.launch {

            _uiState.update { it.copy(status = CreateOfferStatus.Submitting, validationMessage = null) }

            if (!uploadPendingPhoto(showErrors = true)) {

                _uiState.update { it.copy(status = CreateOfferStatus.Idle) }

                return@launch

            }

            val offerId = ensureSavedOfferId(_uiState.value.draft) ?: run {

                _uiState.update { it.copy(status = CreateOfferStatus.Idle) }

                return@launch

            }

            when (val result = repository.submitOffer(offerId, merchantConfirmed = true)) {

                is Result.Success -> {

                    _uiState.update {

                        it.copy(status = CreateOfferStatus.Submitted, draft = ShopOwnerMapper.draftFromOffer(result.data))

                    }

                }

                is Result.Failure -> {

                    _uiState.update {

                        it.copy(

                            status = CreateOfferStatus.Idle,

                            error = result.error,

                            validationMessage = result.error.userMessage(),

                        )

                    }

                }

            }

        }

    }



    private suspend fun uploadPendingPhoto(showErrors: Boolean): Boolean {
        val state = _uiState.value
        if (state.pendingPhotoUri == null && state.pendingPhotoBytes == null) {
            return true
        }
        _uiState.update {
            it.copy(isUploadingPhoto = true, photoErrorMessage = null, photoSaveSucceeded = false)
        }
        val offerId = ensureSavedOfferId(_uiState.value.draft)
        if (offerId == null) {
            _uiState.update { it.copy(isUploadingPhoto = false) }
            return false
        }
        val bytes = resolvePendingPhotoBytes()
        if (bytes == null || bytes.isEmpty()) {
            if (showErrors) {
                _uiState.update {
                    it.copy(
                        isUploadingPhoto = false,
                        photoErrorMessage = runCatching {
                            getApplication<Application>().getString(R.string.offer_photo_compress_failed)
                        }.getOrDefault(COMPRESS_FAILED_FALLBACK),
                    )
                }
            } else {
                _uiState.update { it.copy(isUploadingPhoto = false) }
            }
            return false
        }
        return when (val result = repository.uploadOfferPhoto(offerId, bytes)) {
            is Result.Success -> {
                val offer = result.data
                _uiState.update {
                    it.copy(
                        isUploadingPhoto = false,
                        pendingPhotoUri = null,
                        pendingPhotoBytes = null,
                        photoSaveSucceeded = true,
                        draft = ShopOwnerMapper.draftFromOffer(offer),
                        loadedOfferStatus = offer.status,
                        requiresReVerification = offer.merchantConfirmedAt == null,
                        merchantConfirmed = offer.merchantConfirmedAt != null,
                    )
                }
                true
            }
            is Result.Failure -> {
                if (showErrors) {
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            photoErrorMessage = result.error.userMessage(),
                        )
                    }
                } else {
                    _uiState.update { it.copy(isUploadingPhoto = false) }
                }
                false
            }
        }
    }

    private suspend fun ensureSavedOfferId(draft: MerchantOfferDraft): String? {

        if (!draft.offerId.isNullOrBlank()) return draft.offerId

        val message = MerchantOfferValidator.validateDraft(draft)

        if (message != null) {

            _uiState.update { it.copy(validationMessage = message) }

            return null

        }

        return when (val saved = repository.createOffer(draft)) {

            is Result.Success -> {

                _uiState.update { it.copy(draft = ShopOwnerMapper.draftFromOffer(saved.data)) }

                saved.data.id

            }

            is Result.Failure -> {

                _uiState.update {

                    it.copy(

                        status = CreateOfferStatus.Idle,

                        error = saved.error,

                        validationMessage = saved.error.userMessage(),

                    )

                }

                null

            }

        }

    }



    private suspend fun resolvePendingPhotoBytes(): ByteArray? {

        _uiState.value.pendingPhotoBytes?.let { return it }

        val uriString = _uiState.value.pendingPhotoUri ?: return null

        val uri = Uri.parse(uriString)

        return withContext(ioDispatcher) {

            compressImage(getApplication(), uri)

        }?.also { bytes ->

            _uiState.update { it.copy(pendingPhotoBytes = bytes) }

        }

    }



    private fun updateDraft(block: (MerchantOfferDraft) -> MerchantOfferDraft) {

        _uiState.update { it.copy(draft = block(it.draft), validationMessage = null) }

    }

}


