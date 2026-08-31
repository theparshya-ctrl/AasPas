package com.aaspas.customer.data.remote

import com.aaspas.customer.data.remote.dto.ApiResponseDto
import com.aaspas.customer.data.remote.dto.MerchantOfferCreateDto
import com.aaspas.customer.data.remote.dto.MerchantOfferDto
import com.aaspas.customer.data.remote.dto.MerchantOfferSubmitDto
import com.aaspas.customer.data.remote.dto.MerchantOfferUpdateDto
import com.aaspas.customer.data.remote.dto.ShopOnboardingCreateDto
import com.aaspas.customer.data.remote.dto.ShopOwnerDashboardDto
import com.aaspas.customer.data.remote.dto.ShopOwnerProfileUpdateDto
import com.aaspas.customer.data.remote.dto.ShopOwnerShopDetailDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ShopOwnerApi {
    @POST("api/v1/shops")
    suspend fun createShop(@Body body: ShopOnboardingCreateDto): ApiResponseDto<ShopOwnerShopDetailDto>

    @GET("api/v1/shops/me/dashboard")
    suspend fun getDashboard(): ApiResponseDto<ShopOwnerDashboardDto>

    @GET("api/v1/shops/{shopId}")
    suspend fun getShop(@Path("shopId") shopId: String): ApiResponseDto<ShopOwnerShopDetailDto>

    @PATCH("api/v1/shops/{shopId}")
    suspend fun updateShop(
        @Path("shopId") shopId: String,
        @Body body: ShopOwnerProfileUpdateDto,
    ): ApiResponseDto<ShopOwnerShopDetailDto>

    @POST("api/v1/shops/{shopId}/submit")
    suspend fun submitShop(@Path("shopId") shopId: String): ApiResponseDto<ShopOwnerShopDetailDto>

    @Multipart
    @POST("api/v1/shops/me/photo")
    suspend fun uploadShopPhoto(
        @Part photo: MultipartBody.Part,
    ): ApiResponseDto<ShopOwnerShopDetailDto>

    @Multipart
    @POST("api/v1/offers/{offerId}/photo")
    suspend fun uploadOfferPhoto(
        @Path("offerId") offerId: String,
        @Part photo: MultipartBody.Part,
    ): ApiResponseDto<MerchantOfferDto>

    @POST("api/v1/offers")
    suspend fun createOffer(@Body body: MerchantOfferCreateDto): ApiResponseDto<MerchantOfferDto>

    @GET("api/v1/offers/shop/{shopId}")
    suspend fun listShopOffers(@Path("shopId") shopId: String): ApiResponseDto<List<MerchantOfferDto>>

    @GET("api/v1/offers/manage/{offerId}")
    suspend fun getOffer(@Path("offerId") offerId: String): ApiResponseDto<MerchantOfferDto>

    @PATCH("api/v1/offers/{offerId}")
    suspend fun updateOffer(
        @Path("offerId") offerId: String,
        @Body body: MerchantOfferUpdateDto,
    ): ApiResponseDto<MerchantOfferDto>

    @POST("api/v1/offers/{offerId}/submit")
    suspend fun submitOffer(
        @Path("offerId") offerId: String,
        @Body body: MerchantOfferSubmitDto,
    ): ApiResponseDto<MerchantOfferDto>
}
