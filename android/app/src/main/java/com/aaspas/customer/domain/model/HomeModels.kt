package com.aaspas.customer.domain.model

data class HomeLocation(
    val latitude: Double?,
    val longitude: Double?,
    val available: Boolean,
)

data class Category(
    val id: String,
    val name: String,
    val slug: String,
    val displayOrder: Int,
)

data class Offer(
    val id: String,
    val title: String,
    val description: String?,
    val photoUrl: String?,
    val discountType: String,
    val discountValue: String,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val shopId: String,
    val shopName: String,
    val distanceKm: Double?,
    val category: String?,
    val isComingSoon: Boolean,
    val isSaved: Boolean = false,
    val isVerified: Boolean = false,
    val sourceType: String = "AASPAS",
    val sourceName: String? = null,
) {
    val isExternal: Boolean
        get() = sourceType.equals("EXTERNAL", ignoreCase = true)
}

data class Shop(
    val id: String,
    val name: String,
    val photoUrl: String?,
    val category: String?,
    val addressArea: String?,
    val distanceKm: Double,
    val latitude: Double,
    val longitude: Double,
    val isSaved: Boolean = false,
)

data class HomeFeed(
    val location: HomeLocation,
    val categories: List<Category>,
    val todayOffers: List<Offer>,
    val comingSoon: List<Offer>,
    val nearbyShops: List<Shop>,
)

data class SearchResults(
    val offers: List<Offer>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
)

data class CategoryOffersFeed(
    val categoryId: String,
    val categoryName: String,
    val categorySlug: String,
    val todayOffers: List<Offer>,
    val comingSoon: List<Offer>,
    val totalActive: Int,
    val totalComingSoon: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
)
