package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.ShopDetailsDto
import com.aaspas.customer.data.remote.dto.ShopOfferItemDto
import com.aaspas.customer.domain.model.BusinessHours
import com.aaspas.customer.domain.model.OfferVisibilityStatus
import com.aaspas.customer.domain.model.ShopDetails
import com.aaspas.customer.domain.model.ShopOfferSummary

object ShopDetailsMapper {
    fun toDomain(dto: ShopDetailsDto): ShopDetails = ShopDetails(
        id = dto.shopId,
        name = dto.shopName,
        description = dto.description,
        photoUrl = dto.photoUrl,
        category = dto.category,
        addressLine1 = dto.addressLine1,
        addressLine2 = dto.addressLine2,
        area = dto.area,
        city = dto.city,
        pincode = dto.pincode,
        latitude = dto.latitude,
        longitude = dto.longitude,
        phone = dto.phone,
        businessHours = dto.businessHours?.let {
            BusinessHours(
                opensAt = it["opens_at"],
                closesAt = it["closes_at"],
            )
        },
        isVerified = dto.isVerified,
        activeOfferCount = dto.activeOfferCount,
        distanceKm = dto.distanceKm,
        todayOffers = dto.todayOffers.map { it.toDomain() },
        comingSoon = dto.comingSoon.map { it.toDomain() },
    )

    private fun ShopOfferItemDto.toDomain() = ShopOfferSummary(
        id = offerId,
        title = title,
        description = description,
        photoUrl = photoUrl,
        discountType = discountType,
        discountValue = discountValue,
        startsAt = startsAt,
        endsAt = endsAt,
        status = OfferDetailsMapper.parseStatus(status),
        isVerified = isVerified,
        sourceType = sourceType,
        sourceName = sourceName,
    )
}
