package com.aaspas.customer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShopOwnerDashboardDto(
    val shop: ShopOwnerShopDetailDto,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("can_edit_profile") val canEditProfile: Boolean = false,
    @SerialName("can_submit_offers") val canSubmitOffers: Boolean = false,
    @SerialName("offer_counts") val offerCounts: OfferStatusCountsDto = OfferStatusCountsDto(),
    @SerialName("status_message") val statusMessage: String? = null,
)

@Serializable
data class ShopOwnerShopDetailDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    val category: String? = null,
    @SerialName("contact_number") val contactNumber: String? = null,
    @SerialName("business_hours") val businessHours: BusinessHoursDto? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    val status: String,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("approved_at") val approvedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("primary_location") val primaryLocation: ShopOwnerLocationDto? = null,
)

@Serializable
data class ShopOwnerLocationDto(
    val id: String,
    @SerialName("shop_id") val shopId: String,
    val label: String,
    @SerialName("address_line1") val addressLine1: String,
    @SerialName("address_line2") val addressLine2: String? = null,
    val city: String,
    val state: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    val country: String,
    val latitude: String? = null,
    val longitude: String? = null,
    @SerialName("is_primary") val isPrimary: Boolean = true,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class BusinessHoursDto(
    @SerialName("opens_at") val opensAt: String,
    @SerialName("closes_at") val closesAt: String,
)

@Serializable
data class OfferStatusCountsDto(
    val draft: Int = 0,
    @SerialName("pending_approval") val pendingApproval: Int = 0,
    val rejected: Int = 0,
    val scheduled: Int = 0,
    val active: Int = 0,
    val expired: Int = 0,
)

@Serializable
data class MerchantOfferDto(
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
    @SerialName("approved_at") val approvedAt: String? = null,
    @SerialName("rejected_at") val rejectedAt: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class MerchantOfferCreateDto(
    @SerialName("shop_id") val shopId: String,
    val title: String,
    val description: String? = null,
    @SerialName("discount_type") val discountType: String,
    @SerialName("discount_value") val discountValue: String,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("applicable_products") val applicableProducts: String? = null,
    @SerialName("min_purchase_amount") val minPurchaseAmount: String? = null,
    val terms: String? = null,
)

@Serializable
data class MerchantOfferUpdateDto(
    val title: String? = null,
    val description: String? = null,
    @SerialName("discount_type") val discountType: String? = null,
    @SerialName("discount_value") val discountValue: String? = null,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("applicable_products") val applicableProducts: String? = null,
    @SerialName("min_purchase_amount") val minPurchaseAmount: String? = null,
    val terms: String? = null,
)

@Serializable
data class MerchantOfferSubmitDto(
    @SerialName("merchant_confirmed") val merchantConfirmed: Boolean,
)

@Serializable
data class ShopOnboardingCreateDto(
    val name: String,
    val category: String,
    val description: String? = null,
    @SerialName("contact_number") val contactNumber: String,
    val address: ShopOwnerAddressCreateDto,
    @SerialName("business_hours") val businessHours: BusinessHoursDto,
    @SerialName("photo_url") val photoUrl: String? = null,
)

@Serializable
data class ShopOwnerAddressCreateDto(
    @SerialName("address_line1") val addressLine1: String,
    @SerialName("address_line2") val addressLine2: String? = null,
    val city: String,
    val state: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    val country: String = "IN",
    val latitude: String,
    val longitude: String,
)

@Serializable
data class ShopOwnerProfileUpdateDto(
    val name: String? = null,
    val category: String? = null,
    val description: String? = null,
    @SerialName("contact_number") val contactNumber: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    val address: ShopOwnerAddressUpdateDto? = null,
    @SerialName("business_hours") val businessHours: BusinessHoursDto? = null,
)

@Serializable
data class ShopOwnerAddressUpdateDto(
    @SerialName("address_line1") val addressLine1: String,
    @SerialName("address_line2") val addressLine2: String? = null,
    val city: String,
    val state: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    val country: String = "IN",
    val latitude: String,
    val longitude: String,
)
