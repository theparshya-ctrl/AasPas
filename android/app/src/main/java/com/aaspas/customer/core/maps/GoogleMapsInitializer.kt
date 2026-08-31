package com.aaspas.customer.core.maps

import android.content.Context
import com.aaspas.customer.BuildConfig
import com.google.android.gms.maps.MapsInitializer

/**
 * Initializes the Google Maps SDK when an API key is configured.
 * Does not log or expose the key.
 */
object GoogleMapsInitializer {
    fun initializeIfConfigured(context: Context) {
        if (BuildConfig.MAPS_API_KEY.isBlank()) return
        runCatching {
            @Suppress("DEPRECATION")
            MapsInitializer.initialize(context.applicationContext)
        }
    }
}
