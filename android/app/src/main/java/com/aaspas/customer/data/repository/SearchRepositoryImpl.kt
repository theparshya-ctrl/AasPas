package com.aaspas.customer.data.repository

import com.aaspas.customer.core.common.Result
import com.aaspas.customer.data.mapper.DiscoveryMapper
import com.aaspas.customer.data.remote.DiscoveryApi
import com.aaspas.customer.domain.model.Category
import com.aaspas.customer.domain.model.SearchResults
import com.aaspas.customer.domain.repository.SearchRepository

class SearchRepositoryImpl(
    private val api: DiscoveryApi,
) : SearchRepository {
    override suspend fun search(
        query: String?,
        categoryId: String?,
        latitude: Double?,
        longitude: Double?,
        page: Int,
    ): Result<SearchResults> {
        return DetailsRepositorySupport.fetch(
            block = {
                api.search(
                    query = query?.takeIf { it.isNotBlank() },
                    categoryId = categoryId,
                    latitude = latitude,
                    longitude = longitude,
                    page = page,
                )
            },
            mapper = DiscoveryMapper::toSearchResults,
        )
    }

    override suspend fun listCategories(): Result<List<Category>> {
        return DetailsRepositorySupport.fetch(
            block = { api.listCategories() },
            mapper = { dtos -> dtos.map(DiscoveryMapper::toCategory) },
        )
    }
}
