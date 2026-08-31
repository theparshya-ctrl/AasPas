package com.aaspas.customer.domain.repository

import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.FavoritesFeed

interface FavoritesRepository {
    suspend fun listFavorites(latitude: Double?, longitude: Double?): Result<FavoritesFeed>
    suspend fun saveOffer(offerId: String): Result<Boolean>
    suspend fun unsaveOffer(offerId: String): Result<Boolean>
    suspend fun saveShop(shopId: String): Result<Boolean>
    suspend fun unsaveShop(shopId: String): Result<Boolean>
}
