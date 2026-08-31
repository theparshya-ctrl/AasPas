package com.aaspas.customer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteActionDto(
    @SerialName("favorite_id") val favoriteId: String,
    @SerialName("offer_id") val offerId: String? = null,
    @SerialName("shop_id") val shopId: String? = null,
    val saved: Boolean,
)

@Serializable
data class FavoriteOfferDto(
    @SerialName("favorite_id") val favoriteId: String,
    @SerialName("offer_id") val offerId: String,
    val title: String,
    val description: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("discount_type") val discountType: String,
    @SerialName("discount_value") val discountValue: String,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    val status: String? = null,
    @SerialName("shop_id") val shopId: String,
    @SerialName("shop_name") val shopName: String,
    val category: String? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("saved_at") val savedAt: String,
)

@Serializable
data class FavoriteShopDto(
    @SerialName("favorite_id") val favoriteId: String,
    @SerialName("shop_id") val shopId: String,
    @SerialName("shop_name") val shopName: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    val category: String? = null,
    @SerialName("address_area") val addressArea: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("saved_at") val savedAt: String,
)

@Serializable
data class FavoritesDataDto(
    val offers: List<FavoriteOfferDto> = emptyList(),
    val shops: List<FavoriteShopDto> = emptyList(),
)
