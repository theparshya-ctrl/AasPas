package com.aaspas.customer.data.repository

import com.aaspas.customer.core.common.Result
import com.aaspas.customer.data.mapper.ShopDetailsMapper
import com.aaspas.customer.data.remote.DetailsApi
import com.aaspas.customer.domain.model.ShopDetails
import com.aaspas.customer.domain.repository.ShopDetailsRepository

class ShopDetailsRepositoryImpl(
    private val api: DetailsApi,
) : ShopDetailsRepository {
    override suspend fun getShopDetails(
        shopId: String,
        latitude: Double?,
        longitude: Double?,
    ): Result<ShopDetails> {
        return DetailsRepositorySupport.fetch(
            block = { api.getShopDetails(shopId, latitude, longitude) },
            mapper = ShopDetailsMapper::toDomain,
        )
    }
}
