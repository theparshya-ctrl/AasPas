package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.BusinessHoursDto
import com.aaspas.customer.data.remote.dto.MerchantOfferCreateDto
import com.aaspas.customer.data.remote.dto.MerchantOfferDto
import com.aaspas.customer.data.remote.dto.MerchantOfferSubmitDto
import com.aaspas.customer.data.remote.dto.MerchantOfferUpdateDto
import com.aaspas.customer.data.remote.dto.OfferStatusCountsDto
import com.aaspas.customer.data.remote.dto.ShopOnboardingCreateDto
import com.aaspas.customer.data.remote.dto.ShopOwnerAddressCreateDto
import com.aaspas.customer.data.remote.dto.ShopOwnerAddressUpdateDto
import com.aaspas.customer.data.remote.dto.ShopOwnerDashboardDto
import com.aaspas.customer.data.remote.dto.ShopOwnerLocationDto
import com.aaspas.customer.data.remote.dto.ShopOwnerProfileUpdateDto
import com.aaspas.customer.data.remote.dto.ShopOwnerShopDetailDto
import com.aaspas.customer.domain.model.MerchantBusinessHours
import com.aaspas.customer.domain.model.MerchantOffer
import com.aaspas.customer.domain.model.MerchantOfferDraft
import com.aaspas.customer.domain.model.MerchantOfferStatus
import com.aaspas.customer.domain.model.MerchantOfferCounts
import com.aaspas.customer.domain.model.MerchantShopLocation
import com.aaspas.customer.domain.model.MerchantShopLocationDraft
import com.aaspas.customer.domain.model.MerchantShopProfile
import com.aaspas.customer.domain.model.MerchantShopStatus
import com.aaspas.customer.domain.model.ShopOwnerDashboard
import com.aaspas.customer.domain.model.Offer
import com.aaspas.customer.domain.shopowner.MerchantOfferDateTime

object ShopOwnerMapper {
    fun toDomain(dto: ShopOwnerDashboardDto): ShopOwnerDashboard {
        val shop = toShopProfile(dto.shop)
        return ShopOwnerDashboard(
            shop = shop,
            isVerified = dto.isVerified,
            canEditProfile = dto.canEditProfile,
            canSubmitOffers = dto.canSubmitOffers,
            offerCounts = toOfferCounts(dto.offerCounts),
            statusMessage = dto.statusMessage,
        )
    }

    fun toShopProfile(dto: ShopOwnerShopDetailDto): MerchantShopProfile {
        return MerchantShopProfile(
            id = dto.id,
            name = dto.name,
            description = dto.description,
            category = dto.category,
            contactNumber = dto.contactNumber,
            businessHours = dto.businessHours?.let { toBusinessHours(it) },
            photoUrl = dto.photoUrl,
            status = mapShopStatus(dto.status),
            rejectionReason = dto.rejectionReason,
            isVerified = dto.approvedAt != null,
            location = dto.primaryLocation?.let { toLocation(it) },
        )
    }

    fun toOfferCounts(dto: OfferStatusCountsDto): MerchantOfferCounts {
        return MerchantOfferCounts(
            draft = dto.draft,
            pendingApproval = dto.pendingApproval,
            rejected = dto.rejected,
            scheduled = dto.scheduled,
            active = dto.active,
            expired = dto.expired,
        )
    }

    fun toOffer(dto: MerchantOfferDto): MerchantOffer {
        return MerchantOffer(
            id = dto.id,
            shopId = dto.shopId,
            title = dto.title,
            description = dto.description,
            discountType = dto.discountType,
            discountValue = dto.discountValue,
            status = mapOfferStatus(dto.status),
            startsAt = dto.startsAt,
            endsAt = dto.endsAt,
            photoUrl = dto.photoUrl,
            applicableProducts = dto.applicableProducts,
            minPurchaseAmount = dto.minPurchaseAmount,
            terms = dto.terms,
            rejectionReason = dto.rejectionReason,
            isVerified = dto.isVerified,
            merchantConfirmedAt = dto.merchantConfirmedAt,
        )
    }

    fun draftFromOffer(offer: MerchantOffer): MerchantOfferDraft {
        return MerchantOfferDraft(
            shopId = offer.shopId,
            offerId = offer.id,
            title = offer.title,
            description = offer.description.orEmpty(),
            discountType = offer.discountType,
            discountValue = offer.discountValue,
            startDate = MerchantOfferDateTime.splitDate(offer.startsAt),
            startTime = MerchantOfferDateTime.splitTime(offer.startsAt),
            endDate = MerchantOfferDateTime.splitDate(offer.endsAt),
            endTime = MerchantOfferDateTime.splitTime(offer.endsAt),
            photoUrl = offer.photoUrl.orEmpty(),
            applicableProducts = offer.applicableProducts.orEmpty(),
            minPurchaseAmount = offer.minPurchaseAmount.orEmpty(),
            terms = offer.terms.orEmpty(),
        )
    }

    fun toCreateDto(draft: MerchantOfferDraft): MerchantOfferCreateDto {
        return MerchantOfferCreateDto(
            shopId = draft.shopId,
            title = draft.title.trim(),
            description = draft.description.trim().takeIf { it.isNotEmpty() },
            discountType = draft.discountType,
            discountValue = draft.discountValue.trim(),
            startsAt = MerchantOfferDateTime.combine(draft.startDate, draft.startTime),
            endsAt = MerchantOfferDateTime.combine(draft.endDate, draft.endTime),
            photoUrl = draft.photoUrl.trim().takeIf { it.isNotEmpty() },
            applicableProducts = draft.applicableProducts.trim().takeIf { it.isNotEmpty() },
            minPurchaseAmount = draft.minPurchaseAmount.trim().takeIf { it.isNotEmpty() },
            terms = draft.terms.trim().takeIf { it.isNotEmpty() },
        )
    }

    fun toUpdateDto(draft: MerchantOfferDraft): MerchantOfferUpdateDto {
        return MerchantOfferUpdateDto(
            title = draft.title.trim(),
            description = draft.description.trim().takeIf { it.isNotEmpty() },
            discountType = draft.discountType,
            discountValue = draft.discountValue.trim(),
            startsAt = MerchantOfferDateTime.combine(draft.startDate, draft.startTime),
            endsAt = MerchantOfferDateTime.combine(draft.endDate, draft.endTime),
            photoUrl = draft.photoUrl.trim().takeIf { it.isNotEmpty() },
            applicableProducts = draft.applicableProducts.trim().takeIf { it.isNotEmpty() },
            minPurchaseAmount = draft.minPurchaseAmount.trim().takeIf { it.isNotEmpty() },
            terms = draft.terms.trim().takeIf { it.isNotEmpty() },
        )
    }

    fun toPreviewOffer(draft: MerchantOfferDraft, shopName: String, shopPhotoUrl: String?): Offer {
        val startsAt = MerchantOfferDateTime.combine(draft.startDate, draft.startTime).orEmpty()
        val endsAt = MerchantOfferDateTime.combine(draft.endDate, draft.endTime).orEmpty()
        val isComingSoon = startsAt.isNotEmpty() && startsAt > java.time.Instant.now().toString()
        return Offer(
            id = draft.offerId ?: "preview",
            title = draft.title.trim(),
            description = draft.description.trim().takeIf { it.isNotEmpty() },
            photoUrl = draft.photoUrl.trim().takeIf { it.isNotEmpty() } ?: shopPhotoUrl,
            discountType = draft.discountType,
            discountValue = draft.discountValue.trim(),
            startsAt = startsAt,
            endsAt = endsAt,
            shopId = draft.shopId,
            shopName = shopName,
            distanceKm = null,
            category = null,
            isComingSoon = isComingSoon,
            isSaved = false,
            isVerified = false,
        )
    }

    fun mapOfferStatus(raw: String): MerchantOfferStatus {
        return when (raw.lowercase()) {
            "draft" -> MerchantOfferStatus.Draft
            "pending_approval" -> MerchantOfferStatus.PendingApproval
            "rejected" -> MerchantOfferStatus.Rejected
            "scheduled" -> MerchantOfferStatus.Scheduled
            "active" -> MerchantOfferStatus.Active
            "expired" -> MerchantOfferStatus.Expired
            else -> MerchantOfferStatus.Unknown
        }
    }

    fun toCreateDto(
        name: String,
        category: String,
        description: String?,
        contactNumber: String,
        photoUrl: String?,
        businessHours: MerchantBusinessHours,
        location: MerchantShopLocationDraft,
    ): ShopOnboardingCreateDto {
        return ShopOnboardingCreateDto(
            name = name.trim(),
            category = category.trim(),
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            contactNumber = contactNumber.trim(),
            photoUrl = photoUrl?.trim()?.takeIf { it.isNotEmpty() },
            businessHours = BusinessHoursDto(opensAt = businessHours.opensAt, closesAt = businessHours.closesAt),
            address = ShopOwnerAddressCreateDto(
                addressLine1 = location.addressLine1.trim(),
                addressLine2 = location.addressLine2.trim().takeIf { it.isNotEmpty() },
                city = location.city.trim(),
                state = location.state.trim().takeIf { it.isNotEmpty() },
                postalCode = location.postalCode.trim().takeIf { it.isNotEmpty() },
                latitude = location.latitude.trim(),
                longitude = location.longitude.trim(),
            ),
        )
    }

    fun toUpdateDto(
        name: String,
        category: String,
        description: String?,
        contactNumber: String,
        photoUrl: String?,
        businessHours: MerchantBusinessHours?,
        location: MerchantShopLocationDraft,
    ): ShopOwnerProfileUpdateDto {
        return ShopOwnerProfileUpdateDto(
            name = name.trim(),
            category = category.trim(),
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            contactNumber = contactNumber.trim(),
            photoUrl = photoUrl?.trim()?.takeIf { it.isNotEmpty() },
            businessHours = businessHours?.let {
                BusinessHoursDto(opensAt = it.opensAt, closesAt = it.closesAt)
            },
            address = ShopOwnerAddressUpdateDto(
                addressLine1 = location.addressLine1.trim(),
                addressLine2 = location.addressLine2.trim().takeIf { it.isNotEmpty() },
                city = location.city.trim(),
                state = location.state.trim().takeIf { it.isNotEmpty() },
                postalCode = location.postalCode.trim().takeIf { it.isNotEmpty() },
                latitude = location.latitude.trim(),
                longitude = location.longitude.trim(),
            ),
        )
    }

    fun toPhotoUpdateDto(photoUrl: String?): ShopOwnerProfileUpdateDto {
        return ShopOwnerProfileUpdateDto(
            photoUrl = photoUrl?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    fun locationDraftFromProfile(location: MerchantShopLocation?): MerchantShopLocationDraft {
        if (location == null) return MerchantShopLocationDraft()
        return MerchantShopLocationDraft(
            addressLine1 = location.addressLine1,
            addressLine2 = location.addressLine2.orEmpty(),
            city = location.city,
            state = location.state.orEmpty(),
            postalCode = location.postalCode.orEmpty(),
            latitude = location.latitude?.toString().orEmpty(),
            longitude = location.longitude?.toString().orEmpty(),
        )
    }

    private fun toBusinessHours(dto: BusinessHoursDto): MerchantBusinessHours {
        return MerchantBusinessHours(opensAt = dto.opensAt, closesAt = dto.closesAt)
    }

    private fun toLocation(dto: ShopOwnerLocationDto): MerchantShopLocation {
        return MerchantShopLocation(
            addressLine1 = dto.addressLine1,
            addressLine2 = dto.addressLine2,
            city = dto.city,
            state = dto.state,
            postalCode = dto.postalCode,
            latitude = dto.latitude?.toDoubleOrNull(),
            longitude = dto.longitude?.toDoubleOrNull(),
        )
    }

    fun mapShopStatus(raw: String): MerchantShopStatus {
        return when (raw.lowercase()) {
            "draft" -> MerchantShopStatus.Draft
            "pending_approval" -> MerchantShopStatus.PendingApproval
            "rejected" -> MerchantShopStatus.Rejected
            "active" -> MerchantShopStatus.Active
            else -> MerchantShopStatus.Unknown
        }
    }
}
