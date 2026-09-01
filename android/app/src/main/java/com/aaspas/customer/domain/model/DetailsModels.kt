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
    val startsAt: String? = null,
    val endsAt: String? = null,
    val status: OfferVisibilityStatus,
    val isVerified: Boolean = false,
    val sourceType: String = "AASPAS",
    val sourceName: String? = null,
    val shop: OfferDetailsShop,
) {
    val isExternal: Boolean
        get() = sourceType.equals("EXTERNAL", ignoreCase = true)
}

data class ShopOfferSummary(
    val id: String,
    val title: String,
    val description: String?,
    val photoUrl: String?,
    val discountType: String,
    val discountValue: String,
    val startsAt: String? = null,
    val endsAt: String? = null,
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
