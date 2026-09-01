package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.FavoriteOfferDto
import com.aaspas.customer.data.remote.dto.FavoriteShopDto
import com.aaspas.customer.data.remote.dto.FavoritesDataDto
import com.aaspas.customer.domain.model.FavoriteOffer
import com.aaspas.customer.domain.model.FavoriteShop
import com.aaspas.customer.domain.model.FavoritesFeed
import com.aaspas.customer.domain.model.Offer
import com.aaspas.customer.domain.model.Shop

object FavoritesMapper {
    fun toDomain(dto: FavoritesDataDto): FavoritesFeed = FavoritesFeed(
        offers = dto.offers.map { it.toDomain() },
        shops = dto.shops.map { it.toDomain() },
    )

    private fun FavoriteOfferDto.toDomain(): FavoriteOffer = FavoriteOffer(
        favoriteId = favoriteId,
        offer = Offer(
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
            isComingSoon = status == "coming_soon",
            isSaved = true,
            isVerified = isVerified,
            sourceType = sourceType,
            sourceName = sourceName,
        ),
        isActive = isActive,
        savedAt = savedAt,
    )

    private fun FavoriteShopDto.toDomain(): FavoriteShop = FavoriteShop(
        favoriteId = favoriteId,
        shop = Shop(
            id = shopId,
            name = shopName,
            photoUrl = photoUrl,
            category = category,
            addressArea = addressArea,
            distanceKm = distanceKm ?: 0.0,
            latitude = latitude ?: 0.0,
            longitude = longitude ?: 0.0,
            isSaved = true,
        ),
        isActive = isActive,
        savedAt = savedAt,
    )
}
