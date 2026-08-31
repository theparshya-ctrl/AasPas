package com.aaspas.customer.presentation.map.provider

import org.junit.Assert.assertEquals
import org.junit.Test

class MapDefaultsTest {

    @Test
    fun `pilot fallback center is pimpri area`() {
        assertEquals(18.6298, MapDefaults.PILOT_FALLBACK_CENTER.latitude, 0.0001)
        assertEquals(73.7997, MapDefaults.PILOT_FALLBACK_CENTER.longitude, 0.0001)
    }

    @Test
    fun `local discovery zoom is neighborhood level`() {
        assertEquals(14.0, MapDefaults.LOCAL_DISCOVERY_ZOOM, 0.0)
    }
}
