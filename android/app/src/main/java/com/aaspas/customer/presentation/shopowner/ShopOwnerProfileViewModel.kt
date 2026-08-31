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
import com.aaspas.customer.domain.model.MerchantBusinessHours
import com.aaspas.customer.domain.model.MerchantShopLocationDraft
import com.aaspas.customer.domain.model.MerchantShopStatus
import com.aaspas.customer.domain.model.ShopOwnerDashboard
import com.aaspas.customer.domain.repository.ShopOwnerRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShopOwnerProfileViewModel(
    application: Application,
    private val repository: ShopOwnerRepository,
    private val compressImage: (Application, Uri) -> ByteArray? = { app, uri ->
        ImageCompressor.compressImage(app, uri)
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AndroidViewModel(application) {

    companion object {
        private const val COMPRESS_FAILED_FALLBACK =
            "Could not read the selected photo. Try another image or retake."
    }

    private val _uiState = MutableStateFlow(ShopOwnerProfileUiState())
    val uiState: StateFlow<ShopOwnerProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = ShopOwnerProfileLoadStatus.Loading, error = null) }
            when (val result = repository.getDashboard()) {
                is Result.Success -> applyDashboard(result.data)
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            status = if (result.error == AppError.Unauthorized) {
                                ShopOwnerProfileLoadStatus.Unauthorized
                            } else {
                                ShopOwnerProfileLoadStatus.Error
                            },
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = updateField { it.copy(name = value, validationMessage = null) }
    fun onCategoryChange(value: String) = updateField { it.copy(category = value, validationMessage = null) }
    fun onDescriptionChange(value: String) = updateField { it.copy(description = value) }
    fun onContactNumberChange(value: String) = updateField { it.copy(contactNumber = value, validationMessage = null) }
    fun onPhotoUrlChange(value: String) = updateField { it.copy(photoUrl = value) }

    fun onPhotoPicked(uri: Uri) {
        _uiState.update {
            it.copy(
                pendingPhotoUri = uri.toString(),
                pendingPhotoBytes = null,
                photoErrorMessage = null,
                photoSaveSucceeded = false,
            )
        }
    }

    fun savePendingPhoto() {
        if (!_uiState.value.canEditPhoto) return
        if (_uiState.value.pendingPhotoUri == null && _uiState.value.pendingPhotoBytes == null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploadingPhoto = true,
                    photoErrorMessage = null,
                    photoSaveSucceeded = false,
                )
            }
            val bytes = resolvePendingPhotoBytes()
            if (bytes == null || bytes.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isUploadingPhoto = false,
                        photoErrorMessage = runCatching {
                            getApplication<Application>().getString(R.string.shop_owner_photo_compress_failed)
                        }.getOrDefault(COMPRESS_FAILED_FALLBACK),
                        status = ShopOwnerProfileLoadStatus.Loaded,
                    )
                }
                return@launch
            }
            when (val result = repository.uploadShopPhoto(bytes)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            pendingPhotoBytes = null,
                            pendingPhotoUri = null,
                            photoUrl = result.data.photoUrl.orEmpty(),
                            photoSaveSucceeded = true,
                            status = ShopOwnerProfileLoadStatus.Loaded,
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            photoErrorMessage = result.error.userMessage(),
                            status = ShopOwnerProfileLoadStatus.Loaded,
                        )
                    }
                }
            }
        }
    }

    fun onPhotoRemoved() {
        val state = _uiState.value
        if (!state.canEditPhoto) return
        if (state.pendingPhotoUri != null || state.pendingPhotoBytes != null) {
            _uiState.update {
                it.copy(
                    pendingPhotoUri = null,
                    pendingPhotoBytes = null,
                    photoErrorMessage = null,
                    photoSaveSucceeded = false,
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploadingPhoto = true,
                    photoErrorMessage = null,
                    photoSaveSucceeded = false,
                )
            }
            when (val result = repository.updateShopPhoto(state.shopId, null)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            photoUrl = "",
                            photoSaveSucceeded = true,
                            status = ShopOwnerProfileLoadStatus.Loaded,
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            photoErrorMessage = result.error.userMessage(),
                            status = ShopOwnerProfileLoadStatus.Loaded,
                        )
                    }
                }
            }
        }
    }

    fun onPhotoSuccessShown() {
        _uiState.update { it.copy(photoSaveSucceeded = false) }
    }

    fun onOpensAtChange(value: String) = updateField { it.copy(opensAt = value) }
    fun onClosesAtChange(value: String) = updateField { it.copy(closesAt = value) }
    fun onAddressLine1Change(value: String) = updateLocation { it.copy(addressLine1 = value) }
    fun onAddressLine2Change(value: String) = updateLocation { it.copy(addressLine2 = value) }
    fun onCityChange(value: String) = updateLocation { it.copy(city = value) }
    fun onStateChange(value: String) = updateLocation { it.copy(state = value) }
    fun onPostalCodeChange(value: String) = updateLocation { it.copy(postalCode = value) }
    fun onLatitudeChange(value: String) = updateLocation { it.copy(latitude = value) }
    fun onLongitudeChange(value: String) = updateLocation { it.copy(longitude = value) }

    fun applyGpsLocation(latitude: Double, longitude: Double) {
        updateLocation {
            it.copy(
                latitude = latitude.toString(),
                longitude = longitude.toString(),
            )
        }
    }

    fun savePhoto() {
        savePendingPhoto()
    }

    fun save() {
        val state = _uiState.value
        if (!state.canEdit) return
        if (state.name.trim().length < 2 || state.category.isBlank() || state.contactNumber.isBlank()) {
            _uiState.update {
                it.copy(validationMessage = "Name, category, and phone are required.")
            }
            return
        }
        if (state.locationDraft.addressLine1.isBlank() || state.locationDraft.city.isBlank()) {
            _uiState.update {
                it.copy(validationMessage = "Address and city are required.")
            }
            return
        }
        if (state.locationDraft.latitude.isBlank() || state.locationDraft.longitude.isBlank()) {
            _uiState.update {
                it.copy(validationMessage = "Shop GPS coordinates are required.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(status = ShopOwnerProfileLoadStatus.Saving, error = null, validationMessage = null)
            }
            val businessHours = if (state.opensAt.isNotBlank() && state.closesAt.isNotBlank()) {
                MerchantBusinessHours(opensAt = state.opensAt.trim(), closesAt = state.closesAt.trim())
            } else {
                null
            }
            if (state.pendingPhotoUri != null || state.pendingPhotoBytes != null) {
                val bytes = resolvePendingPhotoBytes()
                if (bytes == null || bytes.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            status = ShopOwnerProfileLoadStatus.Error,
                            photoErrorMessage = getApplication<Application>().getString(
                                R.string.shop_owner_photo_compress_failed,
                            ),
                        )
                    }
                    return@launch
                }
                when (val upload = repository.uploadShopPhoto(bytes)) {
                    is Result.Failure -> {
                        _uiState.update {
                            it.copy(
                                status = ShopOwnerProfileLoadStatus.Error,
                                error = upload.error,
                                photoErrorMessage = upload.error.userMessage(),
                            )
                        }
                        return@launch
                    }
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                pendingPhotoBytes = null,
                                pendingPhotoUri = null,
                                photoUrl = upload.data.photoUrl.orEmpty(),
                            )
                        }
                    }
                }
            }
            when (
                val result = repository.updateShopProfile(
                    shopId = state.shopId,
                    name = state.name,
                    category = state.category,
                    description = state.description,
                    contactNumber = state.contactNumber,
                    photoUrl = _uiState.value.photoUrl.takeIf { it.isNotBlank() },
                    businessHours = businessHours,
                    location = state.locationDraft,
                )
            ) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(shouldNavigateBack = true)
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            status = ShopOwnerProfileLoadStatus.Error,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    fun onNavigateBackHandled() {
        _uiState.update { it.copy(shouldNavigateBack = false) }
    }

    private suspend fun resolvePendingPhotoBytes(): ByteArray? {
        val state = _uiState.value
        state.pendingPhotoBytes?.let { return it }
        val uriString = state.pendingPhotoUri ?: return null
        return withContext(ioDispatcher) {
            compressImage(getApplication(), Uri.parse(uriString))
        }
    }

    private fun applyDashboard(dashboard: ShopOwnerDashboard) {
        val shop = dashboard.shop
        val hours = shop.businessHours
        _uiState.update {
            it.copy(
                status = ShopOwnerProfileLoadStatus.Loaded,
                shopId = shop.id,
                canEdit = dashboard.canEditProfile,
                canEditPhoto = shop.status != MerchantShopStatus.PendingApproval,
                isUploadingPhoto = false,
                pendingPhotoBytes = null,
                pendingPhotoUri = null,
                name = shop.name,
                category = shop.category.orEmpty(),
                description = shop.description.orEmpty(),
                contactNumber = shop.contactNumber.orEmpty(),
                photoUrl = shop.photoUrl.orEmpty(),
                opensAt = hours?.opensAt.orEmpty(),
                closesAt = hours?.closesAt.orEmpty(),
                locationDraft = ShopOwnerMapper.locationDraftFromProfile(shop.location),
                shopStatus = shop.status.name,
                statusMessage = dashboard.statusMessage,
                error = null,
            )
        }
    }

    private fun updateField(block: (ShopOwnerProfileUiState) -> ShopOwnerProfileUiState) {
        _uiState.update(block)
    }

    private fun updateLocation(block: (MerchantShopLocationDraft) -> MerchantShopLocationDraft) {
        _uiState.update { it.copy(locationDraft = block(it.locationDraft), validationMessage = null) }
    }
}
