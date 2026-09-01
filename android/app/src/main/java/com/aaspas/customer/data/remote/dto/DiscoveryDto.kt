package com.aaspas.customer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchOfferDto(
    @SerialName("offer_id") val offerId: String,
    val title: String,
    val description: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("discount_type") val discountType: String,
    @SerialName("discount_value") val discountValue: String,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    val status: String,
    @SerialName("shop_id") val shopId: String,
    @SerialName("shop_name") val shopName: String,
    val category: String? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("is_saved") val isSaved: Boolean = false,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("source_type") val sourceType: String = "AASPAS",
    @SerialName("source_name") val sourceName: String? = null,
)

@Serializable
data class SearchResultsDto(
    val offers: List<SearchOfferDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    @SerialName("total_pages") val totalPages: Int = 0,
)

@Serializable
data class CategoryOffersDto(
    @SerialName("category_id") val categoryId: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("category_slug") val categorySlug: String,
    @SerialName("today_offers") val todayOffers: List<SearchOfferDto> = emptyList(),
    @SerialName("coming_soon") val comingSoon: List<SearchOfferDto> = emptyList(),
    @SerialName("total_active") val totalActive: Int = 0,
    @SerialName("total_coming_soon") val totalComingSoon: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    @SerialName("total_pages") val totalPages: Int = 0,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val slug: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("display_order") val displayOrder: Int = 0,
)
