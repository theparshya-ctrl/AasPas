package com.aaspas.customer.core.startup

import android.os.SystemClock
import android.util.Log
import com.aaspas.customer.BuildConfig

/** Lightweight DEV-only startup timing markers. */
object StartupTracer {
    private val processStartMs: Long = currentElapsedMs()

    fun mark(label: String) {
        if (!isEnabled()) return
        runCatching {
            Log.d(TAG, "$label +${currentElapsedMs() - processStartMs}ms")
        }
    }

    private fun isEnabled(): Boolean {
        return runCatching { BuildConfig.DEBUG }.getOrDefault(false)
    }

    private fun currentElapsedMs(): Long {
        return runCatching { SystemClock.elapsedRealtime() }
            .getOrElse { System.currentTimeMillis() }
    }

    private const val TAG = "StartupTracer"
}
