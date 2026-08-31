package com.aaspas.customer.presentation.map.provider

import com.aaspas.customer.presentation.map.provider.MapProviderType.Google
import com.aaspas.customer.presentation.map.provider.MapProviderType.OpenStreetMap
import org.junit.Assert.assertEquals
import org.junit.Test

class MapProviderSelectorTest {

    @Test
    fun `selects google when key configured and init not failed`() {
        assertEquals(Google, MapProviderSelector.resolve(googleMapsKeyConfigured = true, googleMapsInitFailed = false))
    }

    @Test
    fun `selects osm when google key missing`() {
        assertEquals(OpenStreetMap, MapProviderSelector.resolve(googleMapsKeyConfigured = false, googleMapsInitFailed = false))
    }

    @Test
    fun `selects osm when google init failed`() {
        assertEquals(OpenStreetMap, MapProviderSelector.resolve(googleMapsKeyConfigured = true, googleMapsInitFailed = true))
    }
}
