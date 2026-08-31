package com.aaspas.customer.data.remote

import com.aaspas.customer.data.remote.dto.ApiResponseDto
import com.aaspas.customer.data.remote.dto.CategoryDto
import com.aaspas.customer.data.remote.dto.CategoryOffersDto
import com.aaspas.customer.data.remote.dto.SearchResultsDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DiscoveryApi {
    @GET("api/v1/search")
    suspend fun search(
        @Query("q") query: String? = null,
        @Query("category_id") categoryId: String? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("radius_km") radiusKm: Double? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): ApiResponseDto<SearchResultsDto>

    @GET("api/v1/categories")
    suspend fun listCategories(): ApiResponseDto<List<CategoryDto>>

    @GET("api/v1/categories/{categoryId}/offers")
    suspend fun categoryOffers(
        @Path("categoryId") categoryId: String,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("radius_km") radiusKm: Double? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): ApiResponseDto<CategoryOffersDto>
}
