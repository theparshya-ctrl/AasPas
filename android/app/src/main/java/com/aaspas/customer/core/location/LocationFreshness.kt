package com.aaspas.customer.core.location

import com.aaspas.customer.domain.model.SelectedLocation
import kotlin.math.abs

/**
 * Location freshness and material-change thresholds for Home startup.
 *
 * Fresh saved GPS: use immediately for one Home load; background refresh may update
 * location without reloading Home unless coordinates move materially.
 */
object LocationFreshness {
    /** Saved GPS older than this is treated as stale and refreshed before Home load. */
    const val STALE_THRESHOLD_MS: Long = 15 * 60 * 1000L

    /** Minimum distance (km) between old and new coordinates to trigger a Home reload. */
    const val MATERIAL_DISTANCE_KM: Double = 0.5

    private const val COORDINATE_EPSILON = 0.0001

    fun isFresh(savedAtEpochMs: Long, nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        if (savedAtEpochMs <= 0L) return false
        return nowEpochMs - savedAtEpochMs <= STALE_THRESHOLD_MS
    }

    fun sameCoordinates(
        latA: Double?,
        lngA: Double?,
        latB: Double?,
        lngB: Double?,
    ): Boolean {
        if (latA == null || lngA == null || latB == null || lngB == null) {
            return latA == latB && lngA == lngB
        }
        return abs(latA - latB) < COORDINATE_EPSILON && abs(lngA - lngB) < COORDINATE_EPSILON
    }

    fun isMaterialChange(
        prior: SelectedLocation,
        latitude: Double,
        longitude: Double,
    ): Boolean {
        if (!prior.hasCoordinates) return true
        val priorLat = prior.latitude ?: return true
        val priorLng = prior.longitude ?: return true
        if (sameCoordinates(priorLat, priorLng, latitude, longitude)) return false
        val distanceKm = haversineKm(priorLat, priorLng, latitude, longitude)
        return distanceKm >= MATERIAL_DISTANCE_KM
    }

    fun isMaterialChange(prior: SelectedLocation, next: SelectedLocation): Boolean {
        if (!next.hasCoordinates) return false
        return isMaterialChange(prior, next.latitude!!, next.longitude!!)
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadiusKm * c
    }
}
