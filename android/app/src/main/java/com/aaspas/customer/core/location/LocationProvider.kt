package com.aaspas.customer.core.location

data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double,
)

interface LocationProvider {
    suspend fun getLastLocation(): GeoCoordinates?
}
