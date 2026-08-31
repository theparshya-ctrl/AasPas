package com.aaspas.customer.domain.model

data class ShopOwnerDashboard(
    val shop: MerchantShopProfile,
    val isVerified: Boolean,
    val canEditProfile: Boolean,
    val canSubmitOffers: Boolean,
    val offerCounts: MerchantOfferCounts,
    val statusMessage: String?,
)

data class MerchantShopProfile(
    val id: String,
    val name: String,
    val description: String?,
    val category: String?,
    val contactNumber: String?,
    val businessHours: MerchantBusinessHours?,
    val photoUrl: String?,
    val status: MerchantShopStatus,
    val rejectionReason: String?,
    val isVerified: Boolean,
    val location: MerchantShopLocation?,
)

data class MerchantShopLocation(
    val addressLine1: String,
    val addressLine2: String?,
    val city: String,
    val state: String?,
    val postalCode: String?,
    val latitude: Double?,
    val longitude: Double?,
)

data class MerchantBusinessHours(
    val opensAt: String,
    val closesAt: String,
)

data class MerchantOfferCounts(
    val draft: Int,
    val pendingApproval: Int,
    val rejected: Int,
    val scheduled: Int,
    val active: Int,
    val expired: Int,
) {
    val comingSoon: Int get() = scheduled
}

enum class MerchantOfferStatus {
    Draft,
    PendingApproval,
    Rejected,
    Scheduled,
    Active,
    Expired,
    Unknown,
}

data class MerchantOffer(
    val id: String,
    val shopId: String,
    val title: String,
    val description: String?,
    val discountType: String,
    val discountValue: String,
    val status: MerchantOfferStatus,
    val startsAt: String?,
    val endsAt: String?,
    val photoUrl: String?,
    val applicableProducts: String?,
    val minPurchaseAmount: String?,
    val terms: String?,
    val rejectionReason: String?,
    val isVerified: Boolean,
    val merchantConfirmedAt: String?,
)

data class MerchantOfferDraft(
    val shopId: String,
    val offerId: String? = null,
    val title: String = "",
    val description: String = "",
    val discountType: String = "percentage",
    val discountValue: String = "",
    val startDate: String = "",
    val startTime: String = "",
    val endDate: String = "",
    val endTime: String = "",
    val photoUrl: String = "",
    val applicableProducts: String = "",
    val minPurchaseAmount: String = "",
    val terms: String = "",
)

enum class MerchantShopStatus {
    Draft,
    PendingApproval,
    Rejected,
    Active,
    Unknown,
}

/** Merchant shop location draft — separate from customer SelectedLocation. */
data class MerchantShopLocationDraft(
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val latitude: String = "",
    val longitude: String = "",
)
