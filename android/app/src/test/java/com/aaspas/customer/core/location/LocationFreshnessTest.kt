package com.aaspas.customer.core.location

import com.aaspas.customer.domain.model.LocationSource
import com.aaspas.customer.domain.model.SelectedLocation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFreshnessTest {

    @Test
    fun `fresh location within threshold`() {
        val now = 1_000_000L
        val savedAt = now - LocationFreshness.STALE_THRESHOLD_MS + 1
        assertTrue(LocationFreshness.isFresh(savedAt, now))
    }

    @Test
    fun `stale location beyond threshold`() {
        val now = 1_000_000L
        val savedAt = now - LocationFreshness.STALE_THRESHOLD_MS - 1
        assertFalse(LocationFreshness.isFresh(savedAt, now))
    }

    @Test
    fun `same coordinates are not material change`() {
        val prior = SelectedLocation("Pimpri", 18.6298, 73.7997, LocationSource.CURRENT_GPS)
        assertFalse(LocationFreshness.isMaterialChange(prior, 18.6298, 73.7997))
    }

    @Test
    fun `large coordinate move is material change`() {
        val prior = SelectedLocation("Pimpri", 18.6298, 73.7997, LocationSource.CURRENT_GPS)
        assertTrue(LocationFreshness.isMaterialChange(prior, 19.0760, 72.8777))
    }
}
