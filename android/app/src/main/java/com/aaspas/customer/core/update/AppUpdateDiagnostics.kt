package com.aaspas.customer.core.update

import android.util.Log
import com.aaspas.customer.BuildConfig

internal object AppUpdateDiagnostics {
    private const val TAG = "AasPasAppUpdate"

    fun log(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun logSkip(reason: String) {
        log("skip: $reason")
    }

    fun logCheckStarted(currentVersionCode: Int) {
        log("check started currentVersionCode=$currentVersionCode environment=${BuildConfig.APP_ENVIRONMENT}")
    }

    fun logApiResult(success: Boolean, latestVersionCode: Int?, error: String? = null) {
        when {
            error != null -> log("api failure: $error")
            success -> log("api success latestVersionCode=$latestVersionCode")
            else -> log("api response success=false")
        }
    }

    fun logEvaluatorResult(updateAvailable: Boolean, latestVersionCode: Int?) {
        log("evaluator updateAvailable=$updateAvailable latestVersionCode=$latestVersionCode")
    }

    fun logDialogTrigger(show: Boolean, reason: String) {
        log("dialog show=$show reason=$reason")
    }
}
