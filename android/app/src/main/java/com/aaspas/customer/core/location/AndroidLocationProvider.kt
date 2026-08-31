package com.aaspas.customer.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class AndroidLocationProvider(
    private val context: Context,
) : LocationProvider {
    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    override suspend fun getLastLocation(): GeoCoordinates? {
        if (!hasLocationPermission()) return null
        return try {
            val location = fusedClient.lastLocation.await()
                ?: fusedClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token,
                ).await()
            location?.let { GeoCoordinates(it.latitude, it.longitude) }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}
