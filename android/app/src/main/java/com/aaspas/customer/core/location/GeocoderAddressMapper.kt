package com.aaspas.customer.core.location

import android.location.Address

internal object GeocoderAddressMapper {
    fun toSearchResult(address: Address, fallbackQuery: String): LocationSearchResult? {
        val displayName = buildDisplayName(address, fallbackQuery) ?: return null
        if (!hasValidCoordinates(address.latitude, address.longitude)) return null
        return LocationSearchResult(
            displayName = displayName,
            latitude = address.latitude,
            longitude = address.longitude,
        )
    }

    fun buildDisplayName(address: Address, fallbackQuery: String): String? {
        val candidates = sequenceOf(
            address.locality,
            address.subLocality,
            address.featureName,
            address.subAdminArea,
            address.adminArea,
            address.getAddressLine(0),
        ).mapNotNull { value ->
            value?.trim()?.takeIf { it.isNotEmpty() }
        }
        return candidates.firstOrNull() ?: fallbackQuery.trim().takeIf { it.isNotEmpty() }
    }

    private fun hasValidCoordinates(latitude: Double, longitude: Double): Boolean {
        if (!latitude.isFinite() || !longitude.isFinite()) return false
        if (latitude == 0.0 && longitude == 0.0) return false
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
}
