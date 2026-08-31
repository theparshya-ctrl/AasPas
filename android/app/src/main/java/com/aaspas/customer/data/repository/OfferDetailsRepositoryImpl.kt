package com.aaspas.customer.data.repository

import com.aaspas.customer.core.common.Result
import com.aaspas.customer.data.mapper.OfferDetailsMapper
import com.aaspas.customer.data.remote.DetailsApi
import com.aaspas.customer.domain.model.OfferDetails
import com.aaspas.customer.domain.repository.OfferDetailsRepository

class OfferDetailsRepositoryImpl(
    private val api: DetailsApi,
) : OfferDetailsRepository {
    override suspend fun getOfferDetails(
        offerId: String,
        latitude: Double?,
        longitude: Double?,
    ): Result<OfferDetails> {
        return DetailsRepositorySupport.fetch(
            block = { api.getOfferDetails(offerId, latitude, longitude) },
            mapper = OfferDetailsMapper::toDomain,
        )
    }
}
