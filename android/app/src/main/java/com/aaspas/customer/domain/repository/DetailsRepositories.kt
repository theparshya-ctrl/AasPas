package com.aaspas.customer.domain.repository

import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.OfferDetails
import com.aaspas.customer.domain.model.ShopDetails

interface OfferDetailsRepository {
    suspend fun getOfferDetails(
        offerId: String,
        latitude: Double? = null,
        longitude: Double? = null,
    ): Result<OfferDetails>
}

interface ShopDetailsRepository {
    suspend fun getShopDetails(
        shopId: String,
        latitude: Double? = null,
        longitude: Double? = null,
    ): Result<ShopDetails>
}
