package com.aaspas.customer.core.update

import com.aaspas.customer.data.remote.AppApi
import kotlinx.coroutines.delay

class AppUpdateChecker(
    private val appApi: AppApi,
) {
    suspend fun checkForUpdate(currentVersionCode: Int): AppUpdateOffer? {
        AppUpdateDiagnostics.logCheckStarted(currentVersionCode)
        return checkForUpdateInternal(currentVersionCode, attempt = 1)
            ?: run {
                AppUpdateDiagnostics.log("retrying after delay")
                delay(RETRY_DELAY_MS)
                checkForUpdateInternal(currentVersionCode, attempt = 2)
            }
    }

    private suspend fun checkForUpdateInternal(
        currentVersionCode: Int,
        attempt: Int,
    ): AppUpdateOffer? {
        return try {
            val response = appApi.getAppVersion()
            val remote = response.data
            AppUpdateDiagnostics.logApiResult(
                success = response.success,
                latestVersionCode = remote?.latestVersionCode,
            )
            if (!response.success) return null
            val offer = AppUpdateEvaluator.evaluate(currentVersionCode, remote)
            AppUpdateDiagnostics.logEvaluatorResult(
                updateAvailable = offer != null,
                latestVersionCode = remote?.latestVersionCode,
            )
            offer
        } catch (error: Exception) {
            AppUpdateDiagnostics.logApiResult(
                success = false,
                latestVersionCode = null,
                error = "${error.javaClass.simpleName} attempt=$attempt",
            )
            null
        }
    }

    private companion object {
        const val RETRY_DELAY_MS = 3_000L
    }
}
