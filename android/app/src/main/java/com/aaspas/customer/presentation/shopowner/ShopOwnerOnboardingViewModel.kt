package com.aaspas.customer.presentation.shopowner



import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import com.aaspas.customer.core.common.AppError

import com.aaspas.customer.core.common.Result

import com.aaspas.customer.core.media.ImageCompressor
import com.aaspas.customer.domain.model.MerchantBusinessHours

import com.aaspas.customer.domain.repository.ShopOwnerRepository

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext



import kotlinx.coroutines.launch

class ShopOwnerOnboardingViewModel(

    private val repository: ShopOwnerRepository,
    private val application: Application? = null,

) : ViewModel() {



    private val _uiState = MutableStateFlow(ShopOwnerOnboardingUiState())

    val uiState: StateFlow<ShopOwnerOnboardingUiState> = _uiState.asStateFlow()



    fun onNameChange(value: String) = updateField { it.copy(name = value, validationMessage = null) }

    fun onCategoryChange(value: String) = updateField { it.copy(category = value, validationMessage = null) }

    fun onDescriptionChange(value: String) = updateField { it.copy(description = value) }

    fun onContactNumberChange(value: String) = updateField { it.copy(contactNumber = value, validationMessage = null) }

    fun onPhotoUrlChange(value: String) = updateField { it.copy(photoUrl = value) }

    fun onPhotoPicked(uri: Uri) {
        updateField { it.copy(pendingPhotoUri = uri.toString(), pendingPhotoBytes = null, photoErrorMessage = null) }
    }

    fun onPhotoSelected(bytes: ByteArray) {
        updateField { it.copy(pendingPhotoBytes = bytes, pendingPhotoUri = null, photoErrorMessage = null) }
    }

    fun onPhotoRemoved() {
        updateField { it.copy(pendingPhotoBytes = null, pendingPhotoUri = null, photoUrl = "", photoErrorMessage = null) }
    }

    fun onOpensAtChange(value: String) = updateField { it.copy(opensAt = value, validationMessage = null) }

    fun onClosesAtChange(value: String) = updateField { it.copy(closesAt = value, validationMessage = null) }

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



    fun createShop() {

        val state = _uiState.value

        if (state.status == ShopOwnerOnboardingStatus.Submitting) return



        validate(state)?.let { message ->
            _uiState.update { it.copy(validationMessage = message, error = null) }
            return
        }

        _uiState.update {
            it.copy(
                status = ShopOwnerOnboardingStatus.Submitting,
                validationMessage = null,
                error = null,
            )
        }

        viewModelScope.launch {
            val pendingPhotoBytes = resolvePendingPhotoBytes(state)
            when (
                val result = repository.createShop(

                    name = state.name,

                    category = state.category,

                    description = state.description,

                    contactNumber = state.contactNumber,

                    photoUrl = null,

                    businessHours = MerchantBusinessHours(

                        opensAt = state.opensAt.trim(),

                        closesAt = state.closesAt.trim(),

                    ),

                    location = state.locationDraft,

                )

            ) {

                is Result.Success -> {
                    val createdShopId = result.data.id
                    if (pendingPhotoBytes != null) {
                        when (val upload = repository.uploadShopPhoto(pendingPhotoBytes)) {
                            is Result.Success -> {
                                _uiState.update {
                                    it.copy(
                                        status = ShopOwnerOnboardingStatus.Success,
                                        createdShopId = createdShopId,
                                        photoUrl = upload.data.photoUrl.orEmpty(),
                                        pendingPhotoBytes = null,
                                        pendingPhotoUri = null,
                                        error = null,
                                    )
                                }
                            }
                            is Result.Failure -> {
                                _uiState.update {
                                    it.copy(
                                        status = ShopOwnerOnboardingStatus.Success,
                                        createdShopId = createdShopId,
                                        photoErrorMessage = "Shop created, but photo upload failed. You can add it from profile.",
                                        error = null,
                                    )
                                }
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                status = ShopOwnerOnboardingStatus.Success,
                                createdShopId = createdShopId,
                                error = null,
                            )
                        }
                    }
                }

                is Result.Failure -> {

                    _uiState.update {

                        it.copy(

                            status = ShopOwnerOnboardingStatus.Error,

                            error = result.error,

                        )

                    }

                }

            }

        }

    }



    fun retryAfterError() {

        _uiState.update {

            it.copy(

                status = ShopOwnerOnboardingStatus.Ready,

                error = null,

            )

        }

    }



    private suspend fun resolvePendingPhotoBytes(state: ShopOwnerOnboardingUiState): ByteArray? {
        state.pendingPhotoBytes?.let { return it }
        val uriString = state.pendingPhotoUri ?: return null
        val app = application ?: return null
        return withContext(Dispatchers.IO) {
            ImageCompressor.compressImage(app, Uri.parse(uriString))
        }
    }

    private fun validate(state: ShopOwnerOnboardingUiState): String? {

        if (state.name.trim().length < 2) {

            return "Shop name must be at least 2 characters."

        }

        if (state.category.isBlank()) {

            return "Category is required."

        }

        if (state.contactNumber.trim().length < 7) {

            return "Phone number must be at least 7 digits."

        }

        if (state.opensAt.isBlank() || state.closesAt.isBlank()) {

            return "Business hours are required."

        }

        if (state.locationDraft.addressLine1.isBlank() || state.locationDraft.city.isBlank()) {

            return "Address and city are required."

        }

        if (state.locationDraft.latitude.isBlank() || state.locationDraft.longitude.isBlank()) {

            return "Shop GPS coordinates are required."

        }

        val latitude = state.locationDraft.latitude.trim().toDoubleOrNull()

        val longitude = state.locationDraft.longitude.trim().toDoubleOrNull()

        if (latitude == null || latitude < -90 || latitude > 90) {

            return "Latitude must be a number between -90 and 90."

        }

        if (longitude == null || longitude < -180 || longitude > 180) {

            return "Longitude must be a number between -180 and 180."

        }

        return null

    }



    private fun updateField(block: (ShopOwnerOnboardingUiState) -> ShopOwnerOnboardingUiState) {

        _uiState.update(block)

    }



    private fun updateLocation(block: (com.aaspas.customer.domain.model.MerchantShopLocationDraft) -> com.aaspas.customer.domain.model.MerchantShopLocationDraft) {

        _uiState.update { it.copy(locationDraft = block(it.locationDraft), validationMessage = null) }

    }

}


