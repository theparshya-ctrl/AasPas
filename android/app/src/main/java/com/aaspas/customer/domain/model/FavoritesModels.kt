package com.aaspas.customer.domain.model

data class FavoriteOffer(
    val favoriteId: String,
    val offer: Offer,
    val isActive: Boolean,
    val savedAt: String,
)

data class FavoriteShop(
    val favoriteId: String,
    val shop: Shop,
    val isActive: Boolean,
    val savedAt: String,
)

data class FavoritesFeed(
    val offers: List<FavoriteOffer>,
    val shops: List<FavoriteShop>,
)
