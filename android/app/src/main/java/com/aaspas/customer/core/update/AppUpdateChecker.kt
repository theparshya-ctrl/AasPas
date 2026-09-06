package com.aaspas.customer.core.update

import com.aaspas.customer.data.remote.AppApi

class AppUpdateChecker(
    private val appApi: AppApi,
) {
    suspend fun checkForUpdate(currentVersionCode: Int): AppUpdateOffer? {
        return try {
            val response = appApi.getAppVersion()
            if (!response.success) return null
            AppUpdateEvaluator.evaluate(currentVersionCode, response.data)
        } catch (_: Exception) {
            null
        }
    }
}
