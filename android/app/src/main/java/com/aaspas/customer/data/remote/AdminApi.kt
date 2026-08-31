package com.aaspas.customer.data.remote

import com.aaspas.customer.data.remote.dto.AdminDashboardDto
import com.aaspas.customer.data.remote.dto.AdminOfferRejectDto
import com.aaspas.customer.data.remote.dto.AdminOfferReviewDto
import com.aaspas.customer.data.remote.dto.AdminShopDetailDto
import com.aaspas.customer.data.remote.dto.AdminShopListItemDto
import com.aaspas.customer.data.remote.dto.AdminShopRejectDto
import com.aaspas.customer.data.remote.dto.AdminShopReviewDto
import com.aaspas.customer.data.remote.dto.AdminUserListItemDto
import com.aaspas.customer.data.remote.dto.ApiResponseDto
import com.aaspas.customer.data.remote.dto.MerchantOfferDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApi {
    @GET("api/v1/admin/dashboard")
    suspend fun getDashboard(): ApiResponseDto<AdminDashboardDto>

    @GET("api/v1/admin/offers/pending")
    suspend fun listPendingOffers(): ApiResponseDto<List<AdminOfferReviewDto>>

    @GET("api/v1/admin/offers/{offerId}")
    suspend fun getOfferReview(@Path("offerId") offerId: String): ApiResponseDto<AdminOfferReviewDto>

    @POST("api/v1/admin/offers/{offerId}/approve")
    suspend fun approveOffer(@Path("offerId") offerId: String): ApiResponseDto<MerchantOfferDto>

    @POST("api/v1/admin/offers/{offerId}/reject")
    suspend fun rejectOffer(
        @Path("offerId") offerId: String,
        @Body body: AdminOfferRejectDto,
    ): ApiResponseDto<MerchantOfferDto>

    @GET("api/v1/admin/shops/pending")
    suspend fun listPendingShops(): ApiResponseDto<List<AdminShopReviewDto>>

    @GET("api/v1/admin/shops/{shopId}")
    suspend fun getShopReview(@Path("shopId") shopId: String): ApiResponseDto<AdminShopReviewDto>

    @POST("api/v1/admin/shops/{shopId}/approve")
    suspend fun approveShop(@Path("shopId") shopId: String): ApiResponseDto<AdminShopReviewDto>

    @POST("api/v1/admin/shops/{shopId}/reject")
    suspend fun rejectShop(
        @Path("shopId") shopId: String,
        @Body body: AdminShopRejectDto,
    ): ApiResponseDto<AdminShopReviewDto>

    @GET("api/v1/admin/shops")
    suspend fun listShops(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null,
    ): ApiResponseDto<List<AdminShopListItemDto>>

    @GET("api/v1/admin/shops/{shopId}/detail")
    suspend fun getShopDetail(@Path("shopId") shopId: String): ApiResponseDto<AdminShopDetailDto>

    @GET("api/v1/admin/users")
    suspend fun listUsers(
        @Query("role") role: String? = null,
        @Query("search") search: String? = null,
    ): ApiResponseDto<List<AdminUserListItemDto>>
}
