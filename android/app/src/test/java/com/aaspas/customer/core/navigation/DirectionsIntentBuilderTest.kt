package com.aaspas.customer.core.navigation

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DirectionsIntentBuilderTest {

    @Test
    fun `builds geo intent with coordinates`() {
        val intent = DirectionsIntentBuilder.build(18.63, 73.80, "Fashion Hub")
        assertNotNull(intent)
        assertEquals(Intent.ACTION_VIEW, intent?.action)
        assertTrue(intent?.dataString?.contains("18.63") == true)
        assertTrue(intent?.dataString?.contains("73.8") == true)
    }

    @Test
    fun `returns null without coordinates`() {
        assertNull(DirectionsIntentBuilder.build(null, 73.80))
        assertNull(DirectionsIntentBuilder.build(18.63, null))
    }

    @Test
    fun `directions uses shop destination coordinates only`() {
        val shopLat = 18.6298
        val shopLng = 73.7997

        val intent = DirectionsIntentBuilder.build(shopLat, shopLng, "Fashion Hub")
        assertNotNull(intent)
        val uri = intent!!.dataString.orEmpty()
        assertTrue(uri.contains("18.6298") || uri.contains("18.629"))
        assertTrue(uri.contains("73.7997") || uri.contains("73.799"))
    }

    @Test
    fun `fallback intent without maps package`() {
        val intent = DirectionsIntentBuilder.buildFallback(18.63, 73.80, "Shop")
        assertNotNull(intent)
        assertNull(intent?.`package`)
    }
}
