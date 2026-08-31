package com.aaspas.customer.domain.map

import com.aaspas.customer.domain.model.HomeFeed
import com.aaspas.customer.domain.model.MapShopPin
import com.aaspas.customer.domain.model.Offer

object MapMarkerGrouper {
    fun groupFromHome(
        feed: HomeFeed,
        categoryFilter: MapCategoryFilter? = null,
    ): List<MapShopPin> {
        val categoryName = resolveCategoryName(feed, categoryFilter)
        val offers = (feed.todayOffers + feed.comingSoon).let { all ->
            if (categoryName == null) {
                all
            } else {
                all.filter { offer ->
                    offer.category?.equals(categoryName, ignoreCase = true) == true
                }
            }
        }
        if (offers.isEmpty()) return emptyList()

        val offersByShop = offers.groupBy { it.shopId }
        return feed.nearbyShops.mapNotNull { shop ->
            val shopOffers = offersByShop[shop.id].orEmpty()
            if (shopOffers.isEmpty()) return@mapNotNull null
            if (categoryName != null &&
                shop.category?.equals(categoryName, ignoreCase = true) != true &&
                shopOffers.none { it.category?.equals(categoryName, ignoreCase = true) == true }
            ) {
                return@mapNotNull null
            }
            val featured = pickFeaturedOffer(shopOffers)
            MapShopPin(
                shop = shop,
                offers = shopOffers,
                featuredOffer = featured,
                activeOfferCount = shopOffers.count { !it.isComingSoon },
            )
        }.sortedBy { it.shop.distanceKm }
    }

    internal fun resolveCategoryName(
        feed: HomeFeed,
        categoryFilter: MapCategoryFilter?,
    ): String? {
        if (categoryFilter == null) return null
        val categoryId = categoryFilter.categoryId?.trim()?.takeIf { it.isNotEmpty() }
        if (categoryId != null) {
            feed.categories.find { it.id == categoryId }?.name?.let { return it }
        }
        return categoryFilter.categoryName?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun pickFeaturedOffer(offers: List<Offer>): Offer? {
        return offers.firstOrNull { !it.isComingSoon } ?: offers.firstOrNull()
    }
}
