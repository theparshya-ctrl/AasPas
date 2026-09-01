package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.CategoryDto
import com.aaspas.customer.data.remote.dto.CategoryOffersDto
import com.aaspas.customer.data.remote.dto.SearchOfferDto
import com.aaspas.customer.data.remote.dto.SearchResultsDto
import com.aaspas.customer.domain.model.Category
import com.aaspas.customer.domain.model.CategoryOffersFeed
import com.aaspas.customer.domain.model.Offer
import com.aaspas.customer.domain.model.SearchResults

object DiscoveryMapper {
    fun toSearchResults(dto: SearchResultsDto): SearchResults = SearchResults(
        offers = dto.offers.map { it.toOffer() },
        total = dto.total,
        page = dto.page,
        pageSize = dto.pageSize,
        totalPages = dto.totalPages,
    )

    fun toCategoryOffers(dto: CategoryOffersDto): CategoryOffersFeed = CategoryOffersFeed(
        categoryId = dto.categoryId,
        categoryName = dto.categoryName,
        categorySlug = dto.categorySlug,
        todayOffers = dto.todayOffers.map { it.toOffer() },
        comingSoon = dto.comingSoon.map { it.toOffer() },
        totalActive = dto.totalActive,
        totalComingSoon = dto.totalComingSoon,
        page = dto.page,
        pageSize = dto.pageSize,
        totalPages = dto.totalPages,
    )

    fun toCategory(dto: CategoryDto): Category = Category(
        id = dto.id,
        name = dto.name,
        slug = dto.slug,
        displayOrder = dto.displayOrder,
    )

    fun SearchOfferDto.toOffer(): Offer = Offer(
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
        isComingSoon = status.equals("coming_soon", ignoreCase = true),
        isSaved = isSaved,
        isVerified = isVerified,
        sourceType = sourceType,
        sourceName = sourceName,
    )
}
