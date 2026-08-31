package com.aaspas.customer.domain.repository

import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.HomeFeed

interface HomeRepository {
    suspend fun getHome(latitude: Double?, longitude: Double?): Result<HomeFeed>
}
