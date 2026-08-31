package com.aaspas.customer.domain.model

enum class OfferVisibilityStatus {
    ACTIVE,
    COMING_SOON,
}

data class OfferDetailsShop(
    val id: String,
    val name: String,
    val photoUrl: String?,
    val category: String?,
    val addressArea: String?,
    val latitude: Double?,
    val longitude: Double?,
    val distanceKm: Double?,
)

data class OfferDetails(
    val id: String,
    val title: String,
    val description: String?,
    val photoUrl: String?,
    val discountType: String,
    val discountValue: String,
    val startsAt: String,
    val endsAt: String,
    val status: OfferVisibilityStatus,
    val isVerified: Boolean = false,
    val shop: OfferDetailsShop,
)

data class ShopOfferSummary(
    val id: String,
    val title: String,
    val description: String?,
    val photoUrl: String?,
    val discountType: String,
    val discountValue: String,
    val startsAt: String,
    val endsAt: String,
    val status: OfferVisibilityStatus,
)

data class BusinessHours(
    val opensAt: String?,
    val closesAt: String?,
)

data class ShopDetails(
    val id: String,
    val name: String,
    val description: String?,
    val photoUrl: String?,
    val category: String?,
    val addressLine1: String?,
    val addressLine2: String?,
    val area: String?,
    val city: String?,
    val pincode: String?,
    val latitude: Double?,
    val longitude: Double?,
    val phone: String?,
    val businessHours: BusinessHours?,
    val isVerified: Boolean,
    val activeOfferCount: Int,
    val distanceKm: Double?,
    val todayOffers: List<ShopOfferSummary>,
    val comingSoon: List<ShopOfferSummary>,
)
