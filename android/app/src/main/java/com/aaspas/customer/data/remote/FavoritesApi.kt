package com.aaspas.customer.data.remote

import com.aaspas.customer.data.remote.dto.ApiResponseDto
import com.aaspas.customer.data.remote.dto.AuthTokenDto
import com.aaspas.customer.data.remote.dto.FavoriteActionDto
import com.aaspas.customer.data.remote.dto.FavoritesDataDto
import com.aaspas.customer.data.remote.dto.LoginRequestDto
import com.aaspas.customer.data.remote.dto.RefreshRequestDto
import com.aaspas.customer.data.remote.dto.RegisterRequestDto
import com.aaspas.customer.data.remote.dto.UserAccountDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): ApiResponseDto<UserAccountDto>

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): ApiResponseDto<AuthTokenDto>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): ApiResponseDto<AuthTokenDto>

    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(): ApiResponseDto<UserAccountDto>
}

interface FavoritesApi {
    @GET("api/v1/favorites")
    suspend fun listFavorites(
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
    ): ApiResponseDto<FavoritesDataDto>

    @POST("api/v1/favorites/offers/{offerId}")
    suspend fun saveOffer(@Path("offerId") offerId: String): ApiResponseDto<FavoriteActionDto>

    @DELETE("api/v1/favorites/offers/{offerId}")
    suspend fun unsaveOffer(@Path("offerId") offerId: String): ApiResponseDto<FavoriteActionDto>

    @POST("api/v1/favorites/shops/{shopId}")
    suspend fun saveShop(@Path("shopId") shopId: String): ApiResponseDto<FavoriteActionDto>

    @DELETE("api/v1/favorites/shops/{shopId}")
    suspend fun unsaveShop(@Path("shopId") shopId: String): ApiResponseDto<FavoriteActionDto>
}
