package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.AdminDashboardDto
import com.aaspas.customer.data.remote.dto.AdminOfferReviewDto
import com.aaspas.customer.data.remote.dto.AdminShopDetailDto
import com.aaspas.customer.data.remote.dto.AdminShopListItemDto
import com.aaspas.customer.data.remote.dto.AdminShopReviewDto
import com.aaspas.customer.data.remote.dto.AdminUserListItemDto
import com.aaspas.customer.domain.model.AdminDashboard
import com.aaspas.customer.domain.model.AdminOfferMerchant
import com.aaspas.customer.domain.model.AdminOfferReview
import com.aaspas.customer.domain.model.AdminOfferShop
import com.aaspas.customer.domain.model.AdminAuditEntry
import com.aaspas.customer.domain.model.AdminShopDetail
import com.aaspas.customer.domain.model.AdminShopListItem
import com.aaspas.customer.domain.model.AdminShopOwner
import com.aaspas.customer.domain.model.AdminShopReview
import com.aaspas.customer.domain.model.AdminUserListItem
import com.aaspas.customer.domain.model.MerchantBusinessHours
import com.aaspas.customer.domain.model.Offer

object AdminMapper {
    fun toDomain(dto: AdminDashboardDto) = AdminDashboard(
        pendingShops = dto.pendingShops,
        pendingOffers = dto.pendingOffers,
        activeShops = dto.activeShops,
        rejectedShops = dto.rejectedShops,
        draftShops = dto.draftShops,
    )

    fun toReview(dto: AdminOfferReviewDto) = AdminOfferReview(
        id = dto.id,
        shopId = dto.shopId,
        title = dto.title,
        description = dto.description,
        discountType = dto.discountType,
        discountValue = dto.discountValue,
        status = dto.status,
        startsAt = dto.startsAt,
        endsAt = dto.endsAt,
        photoUrl = dto.photoUrl,
        applicableProducts = dto.applicableProducts,
        minPurchaseAmount = dto.minPurchaseAmount,
        terms = dto.terms,
        merchantConfirmedAt = dto.merchantConfirmedAt,
        submittedAt = dto.submittedAt,
        isVerified = dto.isVerified,
        shop = AdminOfferShop(
            shopId = dto.shop.shopId,
            shopName = dto.shop.shopName,
            category = dto.shop.category,
            status = dto.shop.status,
            isVerified = dto.shop.isVerified,
            photoUrl = dto.shop.photoUrl,
        ),
        merchant = AdminOfferMerchant(
            userId = dto.merchant.userId,
            fullName = dto.merchant.fullName,
            email = dto.merchant.email,
        ),
    )

    fun toPreviewOffer(review: AdminOfferReview): Offer {
        val startsAt = review.startsAt.orEmpty()
        val isComingSoon = startsAt.isNotEmpty() && startsAt > java.time.Instant.now().toString()
        return Offer(
            id = review.id,
            title = review.title,
            description = review.description,
            photoUrl = review.photoUrl ?: review.shop.photoUrl,
            discountType = review.discountType,
            discountValue = review.discountValue,
            startsAt = startsAt,
            endsAt = review.endsAt.orEmpty(),
            shopId = review.shopId,
            shopName = review.shop.shopName,
            distanceKm = null,
            category = review.shop.category,
            isComingSoon = isComingSoon,
            isSaved = false,
            isVerified = false,
        )
    }

    fun toShopReview(dto: AdminShopReviewDto) = AdminShopReview(
        shopId = dto.shopId,
        shopName = dto.shopName,
        description = dto.description,
        category = dto.category,
        photoUrl = dto.photoUrl,
        contactNumber = dto.contactNumber,
        businessHours = dto.businessHours?.let { MerchantBusinessHours(it.opensAt, it.closesAt) },
        status = dto.status,
        submittedAt = dto.submittedAt,
        approvedAt = dto.approvedAt,
        rejectionReason = dto.rejectionReason,
        isVerified = dto.isVerified,
        addressLine1 = dto.addressLine1,
        addressLine2 = dto.addressLine2,
        area = dto.area,
        city = dto.city,
        pincode = dto.pincode,
        latitude = dto.latitude,
        longitude = dto.longitude,
        owner = AdminShopOwner(
            userId = dto.owner.userId,
            fullName = dto.owner.fullName,
            email = dto.owner.email,
            phone = dto.owner.phone,
        ),
    )

    fun toShopListItem(dto: AdminShopListItemDto) = AdminShopListItem(
        shopId = dto.shopId,
        shopName = dto.shopName,
        ownerName = dto.ownerName,
        ownerEmail = dto.ownerEmail,
        category = dto.category,
        city = dto.city,
        status = dto.status,
        isVerified = dto.isVerified,
        createdAt = dto.createdAt,
        offerCount = dto.offerCount,
        rejectionReason = dto.rejectionReason,
    )

    fun toShopDetail(dto: AdminShopDetailDto) = AdminShopDetail(
        shopId = dto.shopId,
        shopName = dto.shopName,
        description = dto.description,
        category = dto.category,
        photoUrl = dto.photoUrl,
        contactNumber = dto.contactNumber,
        businessHours = dto.businessHours?.let { MerchantBusinessHours(it.opensAt, it.closesAt) },
        status = dto.status,
        submittedAt = dto.submittedAt,
        approvedAt = dto.approvedAt,
        rejectionReason = dto.rejectionReason,
        isVerified = dto.isVerified,
        addressLine1 = dto.addressLine1,
        addressLine2 = dto.addressLine2,
        area = dto.area,
        city = dto.city,
        pincode = dto.pincode,
        latitude = dto.latitude,
        longitude = dto.longitude,
        owner = AdminShopOwner(
            userId = dto.owner.userId,
            fullName = dto.owner.fullName,
            email = dto.owner.email,
            phone = dto.owner.phone,
        ),
        createdAt = dto.createdAt,
        offerCount = dto.offerCount,
        offerCountsByStatus = dto.offerCountsByStatus,
        auditEntries = dto.auditEntries.map {
            AdminAuditEntry(
                message = it.message,
                action = it.action,
                actorRole = it.actorRole,
                createdAt = it.createdAt,
            )
        },
    )

    fun toUserListItem(dto: AdminUserListItemDto) = AdminUserListItem(
        userId = dto.userId,
        fullName = dto.fullName,
        email = dto.email,
        role = dto.role,
        isActive = dto.isActive,
        shopId = dto.shopId,
        shopName = dto.shopName,
        createdAt = dto.createdAt,
    )
}
