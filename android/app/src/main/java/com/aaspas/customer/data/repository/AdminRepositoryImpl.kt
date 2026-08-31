package com.aaspas.customer.data.repository

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.core.common.toAppError
import com.aaspas.customer.data.mapper.AdminMapper
import com.aaspas.customer.data.remote.AdminApi
import com.aaspas.customer.data.remote.dto.AdminOfferRejectDto
import com.aaspas.customer.data.remote.dto.AdminShopRejectDto
import com.aaspas.customer.domain.model.AdminDashboard
import com.aaspas.customer.domain.model.AdminOfferReview
import com.aaspas.customer.domain.model.AdminShopDetail
import com.aaspas.customer.domain.model.AdminShopListItem
import com.aaspas.customer.domain.model.AdminShopReview
import com.aaspas.customer.domain.model.AdminUserListItem
import com.aaspas.customer.domain.repository.AdminRepository
import retrofit2.HttpException

class AdminRepositoryImpl(
    private val api: AdminApi,
) : AdminRepository {
    override suspend fun getDashboard(): Result<AdminDashboard> {
        return fetch { api.getDashboard() }.map { AdminMapper.toDomain(it) }
    }

    override suspend fun listPendingOffers(): Result<List<AdminOfferReview>> {
        return fetch { api.listPendingOffers() }.map { list -> list.map(AdminMapper::toReview) }
    }

    override suspend fun getOfferReview(offerId: String): Result<AdminOfferReview> {
        return fetch { api.getOfferReview(offerId) }.map { AdminMapper.toReview(it) }
    }

    override suspend fun approveOffer(offerId: String): Result<Unit> {
        return fetch { api.approveOffer(offerId) }.map { }
    }

    override suspend fun rejectOffer(offerId: String, reason: String): Result<Unit> {
        return fetch { api.rejectOffer(offerId, AdminOfferRejectDto(reason.trim())) }.map { }
    }

    override suspend fun listPendingShops(): Result<List<AdminShopReview>> {
        return fetch { api.listPendingShops() }.map { list -> list.map(AdminMapper::toShopReview) }
    }

    override suspend fun getShopReview(shopId: String): Result<AdminShopReview> {
        return fetch { api.getShopReview(shopId) }.map { AdminMapper.toShopReview(it) }
    }

    override suspend fun approveShop(shopId: String): Result<Unit> {
        return fetch { api.approveShop(shopId) }.map { }
    }

    override suspend fun rejectShop(shopId: String, reason: String): Result<Unit> {
        return fetch { api.rejectShop(shopId, AdminShopRejectDto(reason.trim())) }.map { }
    }

    override suspend fun listShops(status: String?, search: String?): Result<List<AdminShopListItem>> {
        return fetch { api.listShops(status = status, search = search?.trim()?.takeIf { it.isNotEmpty() }) }
            .map { list -> list.map(AdminMapper::toShopListItem) }
    }

    override suspend fun getShopDetail(shopId: String): Result<AdminShopDetail> {
        return fetch { api.getShopDetail(shopId) }.map { AdminMapper.toShopDetail(it) }
    }

    override suspend fun listUsers(role: String?, search: String?): Result<List<AdminUserListItem>> {
        return fetch { api.listUsers(role = role, search = search?.trim()?.takeIf { it.isNotEmpty() }) }
            .map { list -> list.map(AdminMapper::toUserListItem) }
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
                422 -> AppError.Unexpected("Invalid request")
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
