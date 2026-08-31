package com.aaspas.customer.core.location

import android.location.Geocoder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocationSearchServiceDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val service = AndroidGeocoderLocationSearchService(context.applicationContext)

    @Test
    fun geocoderIsPresentOnDevice() {
        assertTrue("Geocoder.isPresent() should be true", Geocoder.isPresent())
    }

    @Test
    fun searchDelhiReturnsResults() = runBlocking {
        assertSearchYieldsResults("Delhi")
    }

    @Test
    fun searchPuneReturnsResults() = runBlocking {
        assertSearchYieldsResults("Pune")
    }

    @Test
    fun searchPimpriChinchwadReturnsResults() = runBlocking {
        assertSearchYieldsResults("Pimpri-Chinchwad")
    }

    @Test
    fun unknownQueryReturnsEmptyNotError() = runBlocking {
        val outcome = service.search("XyzNoPlace99999")
        assertTrue(
            "Unknown place should be Empty, got $outcome",
            outcome is LocationSearchOutcome.Empty,
        )
    }

    private suspend fun assertSearchYieldsResults(query: String) {
        val outcome = service.search(query)
        when (outcome) {
            is LocationSearchOutcome.Success -> {
                assertTrue("$query should return at least one result", outcome.results.isNotEmpty())
                val first = outcome.results.first()
                assertNotEquals("$query latitude invalid", 0.0, first.latitude, 0.0001)
                assertNotEquals("$query longitude invalid", 0.0, first.longitude, 0.0001)
            }
            LocationSearchOutcome.Empty -> {
                // Raw geocoder probe for diagnosis
                @Suppress("DEPRECATION")
                val raw = Geocoder(context, Locale.getDefault()).getFromLocationName(query, 5).orEmpty()
                throw AssertionError(
                    "$query: service returned Empty but raw Geocoder returned ${raw.size} addresses. " +
                        "First address lines: ${raw.firstOrNull()?.getAddressLine(0)} " +
                        "locality=${raw.firstOrNull()?.locality} subAdmin=${raw.firstOrNull()?.subAdminArea}",
                )
            }
            LocationSearchOutcome.GeocoderUnavailable -> {
                throw AssertionError("$query: Geocoder unavailable on device")
            }
            is LocationSearchOutcome.Failure -> {
                throw AssertionError("$query: search failed: ${outcome.cause?.message}", outcome.cause)
            }
        }
    }
}
