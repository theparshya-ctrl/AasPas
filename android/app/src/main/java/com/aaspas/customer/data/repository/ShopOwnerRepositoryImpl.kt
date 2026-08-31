package com.aaspas.customer.data.repository

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.core.common.toAppError
import com.aaspas.customer.data.mapper.ShopOwnerMapper
import com.aaspas.customer.data.remote.ShopOwnerApi
import com.aaspas.customer.data.remote.readApiErrorMessage
import com.aaspas.customer.data.remote.dto.MerchantOfferSubmitDto
import com.aaspas.customer.domain.model.MerchantBusinessHours
import com.aaspas.customer.domain.model.MerchantOffer
import com.aaspas.customer.domain.model.MerchantOfferDraft
import com.aaspas.customer.domain.model.MerchantShopLocationDraft
import com.aaspas.customer.domain.model.MerchantShopProfile
import com.aaspas.customer.domain.model.ShopOwnerDashboard
import com.aaspas.customer.domain.repository.ShopOwnerRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class ShopOwnerRepositoryImpl(
    private val api: ShopOwnerApi,
) : ShopOwnerRepository {
    override suspend fun getDashboard(): Result<ShopOwnerDashboard> {
        return fetch { api.getDashboard() }.map { ShopOwnerMapper.toDomain(it) }
    }

    override suspend fun createShop(
        name: String,
        category: String,
        description: String?,
        contactNumber: String,
        photoUrl: String?,
        businessHours: MerchantBusinessHours,
        location: MerchantShopLocationDraft,
    ): Result<MerchantShopProfile> {
        val body = ShopOwnerMapper.toCreateDto(
            name = name,
            category = category,
            description = description,
            contactNumber = contactNumber,
            photoUrl = photoUrl,
            businessHours = businessHours,
            location = location,
        )
        return fetch { api.createShop(body) }.map { ShopOwnerMapper.toShopProfile(it) }
    }

    override suspend fun updateShopProfile(
        shopId: String,
        name: String,
        category: String,
        description: String?,
        contactNumber: String,
        photoUrl: String?,
        businessHours: MerchantBusinessHours?,
        location: MerchantShopLocationDraft,
    ): Result<MerchantShopProfile> {
        val body = ShopOwnerMapper.toUpdateDto(
            name = name,
            category = category,
            description = description,
            contactNumber = contactNumber,
            photoUrl = photoUrl,
            businessHours = businessHours,
            location = location,
        )
        return fetch { api.updateShop(shopId, body) }.map { ShopOwnerMapper.toShopProfile(it) }
    }

    override suspend fun updateShopPhoto(shopId: String, photoUrl: String?): Result<MerchantShopProfile> {
        val body = ShopOwnerMapper.toPhotoUpdateDto(photoUrl)
        return fetch { api.updateShop(shopId, body) }.map { ShopOwnerMapper.toShopProfile(it) }
    }

    override suspend fun uploadShopPhoto(imageBytes: ByteArray): Result<MerchantShopProfile> {
        val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("file", "shop-photo.jpg", requestBody)
        return fetch { api.uploadShopPhoto(part) }.map { ShopOwnerMapper.toShopProfile(it) }
    }

    override suspend fun uploadOfferPhoto(offerId: String, imageBytes: ByteArray): Result<MerchantOffer> {
        val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("file", "offer-photo.jpg", requestBody)
        return fetch { api.uploadOfferPhoto(offerId, part) }.map { ShopOwnerMapper.toOffer(it) }
    }

    override suspend fun listOffers(shopId: String): Result<List<MerchantOffer>> {
        return fetch { api.listShopOffers(shopId) }.map { list -> list.map(ShopOwnerMapper::toOffer) }
    }

    override suspend fun getOffer(offerId: String): Result<MerchantOffer> {
        return fetch { api.getOffer(offerId) }.map { ShopOwnerMapper.toOffer(it) }
    }

    override suspend fun createOffer(draft: MerchantOfferDraft): Result<MerchantOffer> {
        return fetch { api.createOffer(ShopOwnerMapper.toCreateDto(draft)) }.map { ShopOwnerMapper.toOffer(it) }
    }

    override suspend fun updateOffer(draft: MerchantOfferDraft): Result<MerchantOffer> {
        val offerId = draft.offerId ?: return Result.Failure(AppError.Unexpected("Missing offer id"))
        return fetch { api.updateOffer(offerId, ShopOwnerMapper.toUpdateDto(draft)) }.map { ShopOwnerMapper.toOffer(it) }
    }

    override suspend fun submitOffer(offerId: String, merchantConfirmed: Boolean): Result<MerchantOffer> {
        return fetch {
            api.submitOffer(offerId, MerchantOfferSubmitDto(merchantConfirmed = merchantConfirmed))
        }.map { ShopOwnerMapper.toOffer(it) }
    }

    override suspend fun submitShop(shopId: String): Result<MerchantShopProfile> {
        return fetch { api.submitShop(shopId) }.map { ShopOwnerMapper.toShopProfile(it) }
    }

    private suspend fun <T> fetch(block: suspend () -> com.aaspas.customer.data.remote.dto.ApiResponseDto<T>): Result<T> {
        return try {
            val response = block()
            if (!response.success || response.data == null) {
                Result.Failure(AppError.Server)
            } else {
                Result.Success(response.data)
            }
        } catch (http: HttpException) {
            val error = when (http.code()) {
                404 -> AppError.NotFound
                401, 403 -> AppError.Unauthorized
                422 -> AppError.Unexpected(http.readApiErrorMessage("Invalid offer details"))
                in 500..599 -> AppError.Server
                else -> AppError.Unexpected(http.readApiErrorMessage("Request failed (${http.code()})"))
            }
            Result.Failure(error)
        } catch (throwable: Throwable) {
            Result.Failure(throwable.toAppError())
        }
    }

    private inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Result.Success -> Result.Success(transform(data))
            is Result.Failure -> Result.Failure(error)
        }
    }
}
