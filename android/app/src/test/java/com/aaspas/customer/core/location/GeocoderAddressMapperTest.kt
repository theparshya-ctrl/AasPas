package com.aaspas.customer.core.location

import android.location.Address
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GeocoderAddressMapperTest {

    @Test
    fun `uses address line when locality fields are missing`() {
        val address = Address(Locale("en", "IN")).apply {
            setAddressLine(0, "Pimpri-Chinchwad, Pune, Maharashtra, India")
            latitude = 18.6298
            longitude = 73.7997
        }

        val result = GeocoderAddressMapper.toSearchResult(address, "Pimpri-Chinchwad")

        assertEquals("Pimpri-Chinchwad, Pune, Maharashtra, India", result?.displayName)
        assertEquals(18.6298, result?.latitude!!, 0.0001)
    }

    @Test
    fun `uses feature name when administrative fields are missing`() {
        val address = Address(Locale("en", "IN")).apply {
            featureName = "Pimpri"
            setAddressLine(0, "Pimpri, Maharashtra, India")
            latitude = 18.6298
            longitude = 73.7997
        }

        val result = GeocoderAddressMapper.toSearchResult(address, "Pimpri")

        assertEquals("Pimpri", result?.displayName)
    }

    @Test
    fun `prefers locality over admin area`() {
        val address = Address(Locale("en", "IN")).apply {
            locality = "Pune"
            adminArea = "Maharashtra"
            latitude = 18.5204
            longitude = 73.8567
        }

        val result = GeocoderAddressMapper.toSearchResult(address, "Pune")

        assertEquals("Pune", result?.displayName)
    }

    @Test
    fun `drops invalid coordinates`() {
        val address = Address(Locale("en", "IN")).apply {
            locality = "Delhi"
            latitude = 0.0
            longitude = 0.0
        }

        assertNull(GeocoderAddressMapper.toSearchResult(address, "Delhi"))
    }

    @Test
    fun `falls back to query when address has no labels`() {
        val address = Address(Locale("en", "IN")).apply {
            latitude = 28.6139
            longitude = 77.2090
        }

        val result = GeocoderAddressMapper.toSearchResult(address, "Delhi")

        assertEquals("Delhi", result?.displayName)
    }
}
