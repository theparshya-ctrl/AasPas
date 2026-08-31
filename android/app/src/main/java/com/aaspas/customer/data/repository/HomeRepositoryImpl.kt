package com.aaspas.customer.data.repository

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.core.common.toAppError
import com.aaspas.customer.data.mapper.HomeMapper
import com.aaspas.customer.data.remote.HomeApi
import com.aaspas.customer.domain.model.HomeFeed
import com.aaspas.customer.domain.repository.HomeRepository
import retrofit2.HttpException

class HomeRepositoryImpl(
    private val api: HomeApi,
) : HomeRepository {
    override suspend fun getHome(latitude: Double?, longitude: Double?): Result<HomeFeed> {
        return try {
            val response = api.getHome(
                latitude = latitude,
                longitude = longitude,
            )
            if (!response.success || response.data == null) {
                Result.Failure(AppError.Server)
            } else {
                Result.Success(HomeMapper.toDomain(response.data))
            }
        } catch (http: HttpException) {
            val error = when (http.code()) {
                401, 403 -> AppError.Unauthorized
                in 500..599 -> AppError.Server
                else -> AppError.Unexpected("Request failed (${http.code()})")
            }
            Result.Failure(error)
        } catch (throwable: Throwable) {
            Result.Failure(throwable.toAppError())
        }
    }
}
