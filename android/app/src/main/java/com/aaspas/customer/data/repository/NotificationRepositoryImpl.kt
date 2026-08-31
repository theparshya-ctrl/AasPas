package com.aaspas.customer.data.repository

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.core.common.toAppError
import com.aaspas.customer.data.mapper.NotificationMapper
import com.aaspas.customer.data.remote.NotificationApi
import com.aaspas.customer.domain.model.AppNotification
import com.aaspas.customer.domain.model.NotificationList
import com.aaspas.customer.domain.repository.NotificationRepository
import retrofit2.HttpException

class NotificationRepositoryImpl(
    private val api: NotificationApi,
) : NotificationRepository {
    override suspend fun listNotifications(unreadOnly: Boolean): Result<NotificationList> {
        return fetch { api.listNotifications(unreadOnly) }.map(NotificationMapper::toDomain)
    }

    override suspend fun markRead(notificationId: String): Result<AppNotification> {
        return fetch { api.markRead(notificationId) }.map { dto -> NotificationMapper.toDomain(dto) }
    }

    override suspend fun markAllRead(): Result<Int> {
        return fetch { api.markAllRead() }.map { it.markedRead }
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
                in 500..599 -> AppError.Server
                else -> AppError.Unexpected("Request failed (${http.code()})")
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
