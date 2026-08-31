package com.aaspas.customer.domain.model

data class AdminDashboard(
    val pendingShops: Int,
    val pendingOffers: Int,
    val activeShops: Int,
    val rejectedShops: Int,
    val draftShops: Int,
)

data class AdminOfferMerchant(
    val userId: String,
    val fullName: String?,
    val email: String,
)

data class AdminOfferShop(
    val shopId: String,
    val shopName: String,
    val category: String?,
    val status: String,
    val isVerified: Boolean,
    val photoUrl: String?,
)

data class AdminOfferReview(
    val id: String,
    val shopId: String,
    val title: String,
    val description: String?,
    val discountType: String,
    val discountValue: String,
    val status: String,
    val startsAt: String?,
    val endsAt: String?,
    val photoUrl: String?,
    val applicableProducts: String?,
    val minPurchaseAmount: String?,
    val terms: String?,
    val merchantConfirmedAt: String?,
    val submittedAt: String?,
    val isVerified: Boolean,
    val shop: AdminOfferShop,
    val merchant: AdminOfferMerchant,
)

data class AdminShopOwner(
    val userId: String,
    val fullName: String?,
    val email: String,
    val phone: String?,
)

data class AdminShopReview(
    val shopId: String,
    val shopName: String,
    val description: String?,
    val category: String?,
    val photoUrl: String?,
    val contactNumber: String?,
    val businessHours: MerchantBusinessHours?,
    val status: String,
    val submittedAt: String?,
    val approvedAt: String?,
    val rejectionReason: String?,
    val isVerified: Boolean,
    val addressLine1: String?,
    val addressLine2: String?,
    val area: String?,
    val city: String?,
    val pincode: String?,
    val latitude: Double?,
    val longitude: Double?,
    val owner: AdminShopOwner,
)

data class AdminShopListItem(
    val shopId: String,
    val shopName: String,
    val ownerName: String?,
    val ownerEmail: String,
    val category: String?,
    val city: String?,
    val status: String,
    val isVerified: Boolean,
    val createdAt: String,
    val offerCount: Int,
    val rejectionReason: String?,
)

data class AdminAuditEntry(
    val message: String?,
    val action: String,
    val actorRole: String?,
    val createdAt: String,
)

data class AdminShopDetail(
    val shopId: String,
    val shopName: String,
    val description: String?,
    val category: String?,
    val photoUrl: String?,
    val contactNumber: String?,
    val businessHours: MerchantBusinessHours?,
    val status: String,
    val submittedAt: String?,
    val approvedAt: String?,
    val rejectionReason: String?,
    val isVerified: Boolean,
    val addressLine1: String?,
    val addressLine2: String?,
    val area: String?,
    val city: String?,
    val pincode: String?,
    val latitude: Double?,
    val longitude: Double?,
    val owner: AdminShopOwner,
    val createdAt: String,
    val offerCount: Int,
    val offerCountsByStatus: Map<String, Int>,
    val auditEntries: List<AdminAuditEntry>,
)

data class AdminUserListItem(
    val userId: String,
    val fullName: String?,
    val email: String,
    val role: String,
    val isActive: Boolean,
    val shopId: String?,
    val shopName: String?,
    val createdAt: String,
)
