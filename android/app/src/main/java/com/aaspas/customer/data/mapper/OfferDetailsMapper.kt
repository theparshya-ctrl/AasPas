package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.OfferDetailsDto
import com.aaspas.customer.domain.model.OfferDetails
import com.aaspas.customer.domain.model.OfferDetailsShop
import com.aaspas.customer.domain.model.OfferVisibilityStatus

object OfferDetailsMapper {
    fun toDomain(dto: OfferDetailsDto): OfferDetails = OfferDetails(
        id = dto.offerId,
        title = dto.title,
        description = dto.description,
        photoUrl = dto.photoUrl,
        discountType = dto.discountType,
        discountValue = dto.discountValue,
        startsAt = dto.startsAt,
        endsAt = dto.endsAt,
        status = parseStatus(dto.status),
        isVerified = dto.isVerified,
        sourceType = dto.sourceType,
        sourceName = dto.sourceName,
        shop = OfferDetailsShop(
            id = dto.shop.shopId,
            name = dto.shop.shopName,
            photoUrl = dto.shop.photoUrl,
            category = dto.shop.category,
            addressArea = dto.shop.addressArea,
            latitude = dto.shop.latitude,
            longitude = dto.shop.longitude,
            distanceKm = dto.shop.distanceKm,
        ),
    )

    fun parseStatus(raw: String): OfferVisibilityStatus = when (raw.lowercase()) {
        "coming_soon" -> OfferVisibilityStatus.COMING_SOON
        else -> OfferVisibilityStatus.ACTIVE
    }
}
