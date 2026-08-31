package com.aaspas.customer.domain.repository

import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.AdminDashboard
import com.aaspas.customer.domain.model.AdminOfferReview
import com.aaspas.customer.domain.model.AdminShopDetail
import com.aaspas.customer.domain.model.AdminShopListItem
import com.aaspas.customer.domain.model.AdminShopReview
import com.aaspas.customer.domain.model.AdminUserListItem

interface AdminRepository {
    suspend fun getDashboard(): Result<AdminDashboard>
    suspend fun listPendingOffers(): Result<List<AdminOfferReview>>
    suspend fun getOfferReview(offerId: String): Result<AdminOfferReview>
    suspend fun approveOffer(offerId: String): Result<Unit>
    suspend fun rejectOffer(offerId: String, reason: String): Result<Unit>
    suspend fun listPendingShops(): Result<List<AdminShopReview>>
    suspend fun getShopReview(shopId: String): Result<AdminShopReview>
    suspend fun approveShop(shopId: String): Result<Unit>
    suspend fun rejectShop(shopId: String, reason: String): Result<Unit>
    suspend fun listShops(status: String? = null, search: String? = null): Result<List<AdminShopListItem>>
    suspend fun getShopDetail(shopId: String): Result<AdminShopDetail>
    suspend fun listUsers(role: String? = null, search: String? = null): Result<List<AdminUserListItem>>
}
