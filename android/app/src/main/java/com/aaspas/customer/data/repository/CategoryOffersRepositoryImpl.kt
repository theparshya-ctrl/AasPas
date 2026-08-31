package com.aaspas.customer.data.repository

import com.aaspas.customer.core.common.Result
import com.aaspas.customer.data.mapper.DiscoveryMapper
import com.aaspas.customer.data.remote.DiscoveryApi
import com.aaspas.customer.domain.model.CategoryOffersFeed
import com.aaspas.customer.domain.repository.CategoryOffersRepository

class CategoryOffersRepositoryImpl(
    private val api: DiscoveryApi,
) : CategoryOffersRepository {
    override suspend fun getCategoryOffers(
        categoryId: String,
        latitude: Double?,
        longitude: Double?,
        page: Int,
    ): Result<CategoryOffersFeed> {
        return DetailsRepositorySupport.fetch(
            block = {
                api.categoryOffers(
                    categoryId = categoryId,
                    latitude = latitude,
                    longitude = longitude,
                    page = page,
                )
            },
            mapper = DiscoveryMapper::toCategoryOffers,
        )
    }
}
