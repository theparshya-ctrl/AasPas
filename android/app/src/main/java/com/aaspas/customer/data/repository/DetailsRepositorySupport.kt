package com.aaspas.customer.data.repository

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.core.common.toAppError
import com.aaspas.customer.data.remote.dto.ApiResponseDto
import retrofit2.HttpException

internal object DetailsRepositorySupport {
    suspend fun <T, R> fetch(
        block: suspend () -> ApiResponseDto<T>,
        mapper: (T) -> R,
    ): Result<R> {
        return try {
            val response = block()
            if (!response.success || response.data == null) {
                Result.Failure(AppError.Server)
            } else {
                Result.Success(mapper(response.data))
            }
        } catch (http: HttpException) {
            val error = when (http.code()) {
                404 -> AppError.NotFound
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
