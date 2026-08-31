package com.aaspas.customer.data.repository

import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.data.remote.DetailsApi
import com.aaspas.customer.data.remote.dto.ApiResponseDto
import com.aaspas.customer.data.remote.dto.ShopDetailsDto
import com.aaspas.customer.data.remote.dto.ShopOfferItemDto
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ShopDetailsRepositoryImplTest {

    @Test
    fun `getShopDetails calls shops endpoint and maps success`() = runTest {
        var requestedShopId: String? = null
        val api = object : DetailsApi {
            override suspend fun getOfferDetails(
                offerId: String,
                latitude: Double?,
                longitude: Double?,
            ): ApiResponseDto<com.aaspas.customer.data.remote.dto.OfferDetailsDto> {
                error("Not used")
            }

            override suspend fun getShopDetails(
                shopId: String,
                latitude: Double?,
                longitude: Double?,
            ): ApiResponseDto<ShopDetailsDto> {
                requestedShopId = shopId
                assertEquals(18.63, latitude)
                assertEquals(73.80, longitude)
                return ApiResponseDto(
                    success = true,
                    data = sampleShopDetailsDto(),
                )
            }
        }

        val repository = ShopDetailsRepositoryImpl(api)
        val result = repository.getShopDetails("shop-abc", 18.63, 73.80)

        assertEquals("shop-abc", requestedShopId)
        assertTrue(result is Result.Success)
        assertEquals("Fashion Hub", (result as Result.Success).data.name)
    }

    @Test
    fun `getShopDetails maps 404 to NotFound`() = runTest {
        val api = object : DetailsApi {
            override suspend fun getOfferDetails(
                offerId: String,
                latitude: Double?,
                longitude: Double?,
            ): ApiResponseDto<com.aaspas.customer.data.remote.dto.OfferDetailsDto> {
                error("Not used")
            }

            override suspend fun getShopDetails(
                shopId: String,
                latitude: Double?,
                longitude: Double?,
            ): ApiResponseDto<ShopDetailsDto> {
                throw HttpException(Response.error<Any>(404, "".toResponseBody(null)))
            }
        }

        val result = ShopDetailsRepositoryImpl(api).getShopDetails("missing-shop", null, null)

        assertTrue(result is Result.Failure)
        assertEquals(AppError.NotFound, (result as Result.Failure).error)
    }

    @Test
    fun `getShopDetails maps network failure`() = runTest {
        val api = object : DetailsApi {
            override suspend fun getOfferDetails(
                offerId: String,
                latitude: Double?,
                longitude: Double?,
            ): ApiResponseDto<com.aaspas.customer.data.remote.dto.OfferDetailsDto> {
                error("Not used")
            }

            override suspend fun getShopDetails(
                shopId: String,
                latitude: Double?,
                longitude: Double?,
            ): ApiResponseDto<ShopDetailsDto> {
                throw java.net.UnknownHostException("offline")
            }
        }

        val result = ShopDetailsRepositoryImpl(api).getShopDetails("shop-abc", null, null)

        assertTrue(result is Result.Failure)
        assertEquals(AppError.NoInternet, (result as Result.Failure).error)
    }

    private fun sampleShopDetailsDto() = ShopDetailsDto(
        shopId = "shop-abc",
        shopName = "Fashion Hub",
        category = "Clothing",
        todayOffers = listOf(
            ShopOfferItemDto(
                offerId = "offer-1",
                title = "Today Deal",
                discountType = "percentage",
                discountValue = "15",
                startsAt = "2025-08-19T10:00:00Z",
                endsAt = "2025-08-25T18:00:00Z",
                status = "active",
            ),
        ),
    )
}
