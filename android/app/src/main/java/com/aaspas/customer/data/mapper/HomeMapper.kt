package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.HomeCategoryDto
import com.aaspas.customer.data.remote.dto.HomeDataDto
import com.aaspas.customer.data.remote.dto.HomeLocationDto
import com.aaspas.customer.data.remote.dto.HomeOfferDto
import com.aaspas.customer.data.remote.dto.HomeShopDto
import com.aaspas.customer.domain.model.Category
import com.aaspas.customer.domain.model.HomeFeed
import com.aaspas.customer.domain.model.HomeLocation
import com.aaspas.customer.domain.model.Offer
import com.aaspas.customer.domain.model.Shop

object HomeMapper {
    fun toDomain(dto: HomeDataDto): HomeFeed = HomeFeed(
        location = dto.location.toDomain(),
        categories = dto.categories.map { it.toDomain() },
        todayOffers = dto.todayOffers.map { it.toDomain(isComingSoon = false) },
        comingSoon = dto.comingSoon.map { it.toDomain(isComingSoon = true) },
        nearbyShops = dto.nearbyShops.map { it.toDomain() },
    )

    private fun HomeLocationDto.toDomain() = HomeLocation(
        latitude = latitude,
        longitude = longitude,
        available = available,
    )

    private fun HomeCategoryDto.toDomain() = Category(
        id = id,
        name = name,
        slug = slug,
        displayOrder = displayOrder,
    )

    private fun HomeOfferDto.toDomain(isComingSoon: Boolean) = Offer(
        id = offerId,
        title = title,
        description = description,
        photoUrl = photoUrl,
        discountType = discountType,
        discountValue = discountValue,
        startsAt = startsAt,
        endsAt = endsAt,
        shopId = shopId,
        shopName = shopName,
        distanceKm = distanceKm,
        category = category,
        isComingSoon = isComingSoon,
        isSaved = isSaved,
        isVerified = isVerified,
        sourceType = sourceType,
        sourceName = sourceName,
    )

    private fun HomeShopDto.toDomain() = Shop(
        id = shopId,
        name = shopName,
        photoUrl = photoUrl,
        category = category,
        addressArea = addressArea,
        distanceKm = distanceKm,
        latitude = latitude,
        longitude = longitude,
        isSaved = isSaved,
    )
}
