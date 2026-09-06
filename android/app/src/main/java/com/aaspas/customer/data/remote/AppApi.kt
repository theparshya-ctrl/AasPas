package com.aaspas.customer.data.remote

import com.aaspas.customer.data.remote.dto.ApiResponseDto
import com.aaspas.customer.data.remote.dto.AppVersionDto
import retrofit2.http.GET

interface AppApi {
    @GET("api/v1/app/version")
    suspend fun getAppVersion(): ApiResponseDto<AppVersionDto>
}
