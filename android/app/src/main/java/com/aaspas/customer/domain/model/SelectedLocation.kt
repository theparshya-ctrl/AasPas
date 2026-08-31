package com.aaspas.customer.domain.model

import com.aaspas.customer.core.location.GeoCoordinates

enum class LocationSource {
    CURRENT_GPS,
    MANUAL,
    NONE,
}

data class SelectedLocation(
    val displayName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val source: LocationSource,
) {
    val hasCoordinates: Boolean = latitude != null && longitude != null

    val coordinates: GeoCoordinates?
        get() = if (hasCoordinates) {
            GeoCoordinates(latitude!!, longitude!!)
        } else {
            null
        }

    companion object {
        val None = SelectedLocation(
            displayName = null,
            latitude = null,
            longitude = null,
            source = LocationSource.NONE,
        )
    }
}
