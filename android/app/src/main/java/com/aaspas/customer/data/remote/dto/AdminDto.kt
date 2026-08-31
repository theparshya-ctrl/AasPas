package com.aaspas.customer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdminDashboardDto(
    @SerialName("pending_shops") val pendingShops: Int = 0,
    @SerialName("pending_offers") val pendingOffers: Int = 0,
    @SerialName("active_shops") val activeShops: Int = 0,
    @SerialName("rejected_shops") val rejectedShops: Int = 0,
    @SerialName("draft_shops") val draftShops: Int = 0,
)

@Serializable
data class AdminOfferMerchantDto(
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String? = null,
    val email: String,
)

@Serializable
data class AdminOfferShopDto(
    @SerialName("shop_id") val shopId: String,
    @SerialName("shop_name") val shopName: String,
    val category: String? = null,
    val status: String,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("photo_url") val photoUrl: String? = null,
)

@Serializable
data class AdminOfferReviewDto(
    val id: String,
    @SerialName("shop_id") val shopId: String,
    val title: String,
    val description: String? = null,
    @SerialName("discount_type") val discountType: String,
    @SerialName("discount_value") val discountValue: String,
    val status: String,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("applicable_products") val applicableProducts: String? = null,
    @SerialName("min_purchase_amount") val minPurchaseAmount: String? = null,
    val terms: String? = null,
    @SerialName("merchant_confirmed_at") val merchantConfirmedAt: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    val shop: AdminOfferShopDto,
    val merchant: AdminOfferMerchantDto,
)

@Serializable
data class AdminOfferRejectDto(
    val reason: String,
)

@Serializable
data class AdminShopOwnerDto(
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String? = null,
    val email: String,
    val phone: String? = null,
)

@Serializable
data class AdminShopReviewDto(
    @SerialName("shop_id") val shopId: String,
    @SerialName("shop_name") val shopName: String,
    val description: String? = null,
    val category: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("contact_number") val contactNumber: String? = null,
    @SerialName("business_hours") val businessHours: BusinessHoursDto? = null,
    val status: String,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("approved_at") val approvedAt: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("address_line1") val addressLine1: String? = null,
    @SerialName("address_line2") val addressLine2: String? = null,
    val area: String? = null,
    val city: String? = null,
    val pincode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val owner: AdminShopOwnerDto,
)

@Serializable
data class AdminShopRejectDto(
    val reason: String,
)

@Serializable
data class AdminShopListItemDto(
    @SerialName("shop_id") val shopId: String,
    @SerialName("shop_name") val shopName: String,
    @SerialName("owner_name") val ownerName: String? = null,
    @SerialName("owner_email") val ownerEmail: String,
    val category: String? = null,
    val city: String? = null,
    val status: String,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("offer_count") val offerCount: Int = 0,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
)

@Serializable
data class AdminAuditEntryDto(
    val message: String? = null,
    val action: String,
    @SerialName("actor_role") val actorRole: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class AdminShopDetailDto(
    @SerialName("shop_id") val shopId: String,
    @SerialName("shop_name") val shopName: String,
    val description: String? = null,
    val category: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("contact_number") val contactNumber: String? = null,
    @SerialName("business_hours") val businessHours: BusinessHoursDto? = null,
    val status: String,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("approved_at") val approvedAt: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("address_line1") val addressLine1: String? = null,
    @SerialName("address_line2") val addressLine2: String? = null,
    val area: String? = null,
    val city: String? = null,
    val pincode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val owner: AdminShopOwnerDto,
    @SerialName("created_at") val createdAt: String,
    @SerialName("offer_count") val offerCount: Int = 0,
    @SerialName("offer_counts_by_status") val offerCountsByStatus: Map<String, Int> = emptyMap(),
    @SerialName("audit_entries") val auditEntries: List<AdminAuditEntryDto> = emptyList(),
)

@Serializable
data class AdminUserListItemDto(
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String? = null,
    val email: String,
    val role: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("shop_id") val shopId: String? = null,
    @SerialName("shop_name") val shopName: String? = null,
    @SerialName("created_at") val createdAt: String,
)
