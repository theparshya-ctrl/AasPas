package com.aaspas.customer.presentation.location

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.aaspas.customer.core.location.AndroidLocationProvider
import com.aaspas.customer.core.location.GeoCoordinates
import com.aaspas.customer.core.location.LocalityResolver
import kotlinx.coroutines.launch

data class ResolvedLocation(
    val coordinates: GeoCoordinates,
    val localityName: String?,
)

@Composable
fun LocationPermissionHandler(
    locationProvider: AndroidLocationProvider,
    localityResolver: LocalityResolver,
    onDenied: () -> Unit,
    onGranted: (ResolvedLocation) -> Unit,
    onUnavailable: () -> Unit,
    onLoading: () -> Unit,
    requestOnLaunch: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    var permissionRequested by remember { mutableStateOf(false) }

    suspend fun resolveAndDeliver() {
        onLoading()
        val coords = locationProvider.getLastLocation()
        if (coords == null) {
            onUnavailable()
        } else {
            val locality = localityResolver.resolveLocality(coords)
            onGranted(ResolvedLocation(coords, locality))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permissionRequested = true
        if (granted) {
            scope.launch { resolveAndDeliver() }
        } else {
            onDenied()
        }
    }

    LaunchedEffect(requestOnLaunch) {
        if (!requestOnLaunch) return@LaunchedEffect
        if (locationProvider.hasLocationPermission()) {
            resolveAndDeliver()
        } else if (!permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }
}

@Composable
fun RequestLocationPermission(
    locationProvider: AndroidLocationProvider,
    localityResolver: LocalityResolver,
    onDenied: () -> Unit,
    onGranted: (ResolvedLocation) -> Unit,
    onUnavailable: () -> Unit,
    onLoading: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                onLoading()
                val coords = locationProvider.getLastLocation()
                if (coords == null) {
                    onUnavailable()
                } else {
                    val locality = localityResolver.resolveLocality(coords)
                    onGranted(ResolvedLocation(coords, locality))
                }
            }
        } else {
            onDenied()
        }
    }

    LaunchedEffect(Unit) {
        if (locationProvider.hasLocationPermission()) {
            onLoading()
            val coords = locationProvider.getLastLocation()
            if (coords == null) {
                onUnavailable()
            } else {
                val locality = localityResolver.resolveLocality(coords)
                onGranted(ResolvedLocation(coords, locality))
            }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }
}
