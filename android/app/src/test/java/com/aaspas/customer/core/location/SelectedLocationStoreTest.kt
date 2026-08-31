package com.aaspas.customer.core.location

import android.content.Context
import com.aaspas.customer.domain.model.LocationSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SelectedLocationStoreTest {

    private lateinit var context: Context
    private lateinit var store: SelectedLocationStore

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("aaspas_selected_location", Context.MODE_PRIVATE).edit().clear().apply()
        store = SelectedLocationStore(context)
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun `starts with no selected location`() {
        assertEquals(LocationSource.NONE, store.current().source)
        assertFalse(store.current().hasCoordinates)
    }

    @Test
    fun `manual selection persists across new store instance`() {
        store.setManual("Pimpri", 18.6298, 73.7997)

        val restored = SelectedLocationStore(context).current()
        assertEquals(LocationSource.MANUAL, restored.source)
        assertEquals("Pimpri", restored.displayName)
        assertEquals(18.6298, restored.latitude!!, 0.0001)
        assertEquals(73.7997, restored.longitude!!, 0.0001)
    }

    @Test
    fun `current gps selection persists`() {
        store.setCurrentGps("Chinchwad", 18.6278, 73.7911)

        val restored = SelectedLocationStore(context).current()
        assertEquals(LocationSource.CURRENT_GPS, restored.source)
        assertEquals("Chinchwad", restored.displayName)
        assertTrue(restored.hasCoordinates)
    }

    @Test
    fun `switching manual to gps updates source`() {
        store.setManual("Pimpri", 18.6298, 73.7997)
        store.setCurrentGps("Chinchwad", 18.6278, 73.7911)

        val current = store.current()
        assertEquals(LocationSource.CURRENT_GPS, current.source)
        assertEquals("Chinchwad", current.displayName)
    }

    @Test
    fun `clear resets to none`() {
        store.setManual("Pimpri", 18.6298, 73.7997)
        store.clear()

        assertEquals(LocationSource.NONE, store.current().source)
        assertFalse(SelectedLocationStore(context).current().hasCoordinates)
    }
}
