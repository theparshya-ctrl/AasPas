package com.aaspas.customer.data.remote

import com.aaspas.customer.data.remote.dto.ApiResponseDto
import com.aaspas.customer.data.remote.dto.HomeDataDto
import retrofit2.http.GET
import retrofit2.http.Query

interface HomeApi {
    @GET("api/v1/home")
    suspend fun getHome(
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("radius_km") radiusKm: Double? = null,
    ): ApiResponseDto<HomeDataDto>
}
