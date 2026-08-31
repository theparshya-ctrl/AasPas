package com.aaspas.customer.presentation.map.provider

import com.aaspas.customer.core.location.GeoCoordinates

object MapDefaults {
    /** Local-commerce discovery zoom — nearby shops visible without zooming in again. */
    const val LOCAL_DISCOVERY_ZOOM = 14.0

    /** Pilot fallback center (Pimpri–Chinchwad area) when no location is available. */
    val PILOT_FALLBACK_CENTER = GeoCoordinates(
        latitude = 18.6298,
        longitude = 73.7997,
    )
}
