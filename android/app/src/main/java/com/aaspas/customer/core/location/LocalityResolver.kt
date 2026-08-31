package com.aaspas.customer.core.location

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class LocalityResolver(
    private val context: Context,
) {
    suspend fun resolveLocality(coordinates: GeoCoordinates): String? {
        if (!Geocoder.isPresent()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.getDefault())
                    .getFromLocation(coordinates.latitude, coordinates.longitude, 1)
                    ?.firstOrNull()
                    ?.let { address ->
                        address.locality ?: address.subAdminArea ?: address.adminArea
                    }
            }.getOrNull()
        }
    }
}
