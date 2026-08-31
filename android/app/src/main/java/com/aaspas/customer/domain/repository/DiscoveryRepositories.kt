package com.aaspas.customer.domain.repository

import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.Category
import com.aaspas.customer.domain.model.CategoryOffersFeed
import com.aaspas.customer.domain.model.SearchResults

interface SearchRepository {
    suspend fun search(
        query: String?,
        categoryId: String?,
        latitude: Double?,
        longitude: Double?,
        page: Int = 1,
    ): Result<SearchResults>

    suspend fun listCategories(): Result<List<Category>>
}

interface CategoryOffersRepository {
    suspend fun getCategoryOffers(
        categoryId: String,
        latitude: Double?,
        longitude: Double?,
        page: Int = 1,
    ): Result<CategoryOffersFeed>
}
