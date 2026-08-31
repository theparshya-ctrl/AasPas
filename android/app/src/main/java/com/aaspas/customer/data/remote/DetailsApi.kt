package com.aaspas.customer.data.remote

import com.aaspas.customer.data.remote.dto.ApiResponseDto
import com.aaspas.customer.data.remote.dto.OfferDetailsDto
import com.aaspas.customer.data.remote.dto.ShopDetailsDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DetailsApi {
    @GET("api/v1/offers/{offerId}")
    suspend fun getOfferDetails(
        @Path("offerId") offerId: String,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
    ): ApiResponseDto<OfferDetailsDto>

    @GET("api/v1/shops/{shopId}")
    suspend fun getShopDetails(
        @Path("shopId") shopId: String,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
    ): ApiResponseDto<ShopDetailsDto>
}
