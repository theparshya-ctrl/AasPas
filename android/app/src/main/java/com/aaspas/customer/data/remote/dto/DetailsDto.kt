package com.aaspas.customer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OfferDetailsShopDto(
    @SerialName("shop_id") val shopId: String,
    @SerialName("shop_name") val shopName: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    val category: String? = null,
    @SerialName("address_area") val addressArea: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
)

@Serializable
data class OfferDetailsDto(
    @SerialName("offer_id") val offerId: String,
    val title: String,
    val description: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("discount_type") val discountType: String,
    @SerialName("discount_value") val discountValue: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val status: String,
    @SerialName("is_verified") val isVerified: Boolean = false,
    val shop: OfferDetailsShopDto,
)

@Serializable
data class ShopOfferItemDto(
    @SerialName("offer_id") val offerId: String,
    val title: String,
    val description: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("discount_type") val discountType: String,
    @SerialName("discount_value") val discountValue: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    val status: String,
)

@Serializable
data class ShopDetailsDto(
    @SerialName("shop_id") val shopId: String,
    @SerialName("shop_name") val shopName: String,
    val description: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    val category: String? = null,
    @SerialName("address_line1") val addressLine1: String? = null,
    @SerialName("address_line2") val addressLine2: String? = null,
    val area: String? = null,
    val city: String? = null,
    val pincode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val phone: String? = null,
    @SerialName("business_hours") val businessHours: Map<String, String>? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("active_offer_count") val activeOfferCount: Int = 0,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("today_offers") val todayOffers: List<ShopOfferItemDto> = emptyList(),
    @SerialName("coming_soon") val comingSoon: List<ShopOfferItemDto> = emptyList(),
)
