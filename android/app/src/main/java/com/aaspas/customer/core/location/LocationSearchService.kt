package com.aaspas.customer.core.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LocationSearchResult(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
)

sealed class LocationSearchOutcome {
    data class Success(val results: List<LocationSearchResult>) : LocationSearchOutcome()
    data object Empty : LocationSearchOutcome()
    data object GeocoderUnavailable : LocationSearchOutcome()
    data class Failure(val cause: Throwable?) : LocationSearchOutcome()
}

interface LocationSearchService {
    suspend fun search(query: String, maxResults: Int = 5): LocationSearchOutcome
}

class AndroidGeocoderLocationSearchService(
    private val context: Context,
) : LocationSearchService {
    override suspend fun search(query: String, maxResults: Int): LocationSearchOutcome {
        val trimmed = query.trim()
        if (trimmed.length < 2) return LocationSearchOutcome.Empty
        if (!Geocoder.isPresent()) return LocationSearchOutcome.GeocoderUnavailable

        return withContext(Dispatchers.IO) {
            runCatching {
                val addresses = geocodeByName(trimmed, maxResults)
                addresses.mapNotNull { address -> GeocoderAddressMapper.toSearchResult(address, trimmed) }
                    .distinctBy { "${it.displayName}|${it.latitude}|${it.longitude}" }
            }.fold(
                onSuccess = { results ->
                    when {
                        results.isEmpty() -> LocationSearchOutcome.Empty
                        else -> LocationSearchOutcome.Success(results)
                    }
                },
                onFailure = { LocationSearchOutcome.Failure(it) },
            )
        }
    }

    private suspend fun geocodeByName(query: String, maxResults: Int): List<Address> {
        val primary = fetchAddresses(Geocoder(context, PRIMARY_LOCALE), query, maxResults)
        if (primary.isNotEmpty()) return primary

        val defaultLocale = Locale.getDefault()
        if (defaultLocale.language == PRIMARY_LOCALE.language &&
            defaultLocale.country == PRIMARY_LOCALE.country
        ) {
            return primary
        }
        return fetchAddresses(Geocoder(context, defaultLocale), query, maxResults)
    }

    private suspend fun fetchAddresses(
        geocoder: Geocoder,
        query: String,
        maxResults: Int,
    ): List<Address> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(
                    query,
                    maxResults,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) {
                                continuation.resume(addresses)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    IOException(errorMessage ?: "Location search failed"),
                                )
                            }
                        }
                    },
                )
            }
        }

        @Suppress("DEPRECATION")
        return geocoder.getFromLocationName(query, maxResults).orEmpty()
    }

    companion object {
        private val PRIMARY_LOCALE: Locale = Locale("en", "IN")
    }
}
