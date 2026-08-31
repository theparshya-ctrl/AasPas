package com.aaspas.customer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponseDto<T>(
    val success: Boolean = false,
    val data: T? = null,
    val meta: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
)

@Serializable
data class HomeLocationDto(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val available: Boolean = false,
)

@Serializable
data class HomeCategoryDto(
    val id: String,
    val name: String,
    val slug: String,
    @SerialName("display_order") val displayOrder: Int = 0,
)

@Serializable
data class HomeOfferDto(
    @SerialName("offer_id") val offerId: String,
    val title: String,
    val description: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("discount_type") val discountType: String,
    @SerialName("discount_value") val discountValue: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    @SerialName("shop_id") val shopId: String,
    @SerialName("shop_name") val shopName: String,
    @SerialName("distance_km") val distanceKm: Double? = null,
    val category: String? = null,
    @SerialName("is_saved") val isSaved: Boolean = false,
    @SerialName("is_verified") val isVerified: Boolean = false,
)

@Serializable
data class HomeShopDto(
    @SerialName("shop_id") val shopId: String,
    @SerialName("shop_name") val shopName: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    val category: String? = null,
    @SerialName("address_area") val addressArea: String? = null,
    @SerialName("distance_km") val distanceKm: Double,
    val latitude: Double,
    val longitude: Double,
    @SerialName("is_saved") val isSaved: Boolean = false,
)

@Serializable
data class HomeDataDto(
    val location: HomeLocationDto,
    val categories: List<HomeCategoryDto> = emptyList(),
    @SerialName("today_offers") val todayOffers: List<HomeOfferDto> = emptyList(),
    @SerialName("coming_soon") val comingSoon: List<HomeOfferDto> = emptyList(),
    @SerialName("nearby_shops") val nearbyShops: List<HomeShopDto> = emptyList(),
)
