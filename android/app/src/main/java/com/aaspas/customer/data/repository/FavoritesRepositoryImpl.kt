package com.aaspas.customer.data.repository

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.core.common.toAppError
import com.aaspas.customer.data.mapper.FavoritesMapper
import com.aaspas.customer.data.remote.FavoritesApi
import com.aaspas.customer.domain.model.FavoritesFeed
import com.aaspas.customer.domain.repository.FavoritesRepository
import retrofit2.HttpException

class FavoritesRepositoryImpl(
    private val api: FavoritesApi,
) : FavoritesRepository {
    override suspend fun listFavorites(latitude: Double?, longitude: Double?): Result<FavoritesFeed> {
        return try {
            val response = api.listFavorites(latitude, longitude)
            if (!response.success || response.data == null) {
                Result.Failure(AppError.Server)
            } else {
                Result.Success(FavoritesMapper.toDomain(response.data))
            }
        } catch (http: HttpException) {
            Result.Failure(when (http.code()) {
                401 -> AppError.Unauthorized
                else -> AppError.Unexpected("Request failed (${http.code()})")
            })
        } catch (throwable: Throwable) {
            Result.Failure(throwable.toAppError())
        }
    }

    override suspend fun saveOffer(offerId: String): Result<Boolean> {
        return toggle { api.saveOffer(offerId) }
    }

    override suspend fun unsaveOffer(offerId: String): Result<Boolean> {
        return toggle { api.unsaveOffer(offerId) }
    }

    override suspend fun saveShop(shopId: String): Result<Boolean> {
        return toggle { api.saveShop(shopId) }
    }

    override suspend fun unsaveShop(shopId: String): Result<Boolean> {
        return toggle { api.unsaveShop(shopId) }
    }

    private suspend fun toggle(
        block: suspend () -> com.aaspas.customer.data.remote.dto.ApiResponseDto<com.aaspas.customer.data.remote.dto.FavoriteActionDto>,
    ): Result<Boolean> {
        return try {
            val response = block()
            if (!response.success || response.data == null) {
                Result.Failure(AppError.Server)
            } else {
                Result.Success(response.data.saved)
            }
        } catch (http: HttpException) {
            Result.Failure(when (http.code()) {
                401 -> AppError.Unauthorized
                else -> AppError.Unexpected("Request failed (${http.code()})")
            })
        } catch (throwable: Throwable) {
            Result.Failure(throwable.toAppError())
        }
    }
}
