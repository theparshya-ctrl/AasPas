package com.aaspas.customer.core.location

import android.content.Context
import com.aaspas.customer.domain.model.LocationSource
import com.aaspas.customer.domain.model.SelectedLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SelectedLocationStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _selectedLocation = MutableStateFlow(loadFromPrefs())
    val selectedLocation: StateFlow<SelectedLocation> = _selectedLocation.asStateFlow()

    fun current(): SelectedLocation = _selectedLocation.value

    /** Epoch millis when the current coordinates were last persisted. */
    fun savedAtEpochMs(): Long = prefs.getLong(KEY_SAVED_AT_MS, 0L)

    fun isLocationFresh(nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        return LocationFreshness.isFresh(savedAtEpochMs(), nowEpochMs)
    }

    fun setManual(displayName: String, latitude: Double, longitude: Double) {
        persist(
            SelectedLocation(
                displayName = displayName.trim(),
                latitude = latitude,
                longitude = longitude,
                source = LocationSource.MANUAL,
            ),
        )
    }

    fun setCurrentGps(displayName: String?, latitude: Double, longitude: Double) {
        persist(
            SelectedLocation(
                displayName = displayName?.trim()?.takeIf { it.isNotEmpty() },
                latitude = latitude,
                longitude = longitude,
                source = LocationSource.CURRENT_GPS,
            ),
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
        _selectedLocation.value = SelectedLocation.None
    }

    /** Updates display name only; coordinates and saved timestamp are unchanged. */
    fun updateDisplayNameOnly(displayName: String?) {
        val current = _selectedLocation.value
        if (!current.hasCoordinates) return
        val trimmed = displayName?.trim()?.takeIf { it.isNotEmpty() }
        prefs.edit().putString(KEY_DISPLAY_NAME, trimmed).apply()
        _selectedLocation.value = current.copy(displayName = trimmed)
    }

    private fun persist(location: SelectedLocation) {
        val nowMs = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_DISPLAY_NAME, location.displayName)
            .putString(KEY_SOURCE, location.source.name)
            .apply {
                if (location.latitude != null && location.longitude != null) {
                    putLong(KEY_LAT_BITS, location.latitude.toRawBits())
                    putLong(KEY_LNG_BITS, location.longitude.toRawBits())
                    putLong(KEY_SAVED_AT_MS, nowMs)
                } else {
                    remove(KEY_LAT_BITS)
                    remove(KEY_LNG_BITS)
                    remove(KEY_SAVED_AT_MS)
                }
            }
            .apply()
        _selectedLocation.value = location
    }

    private fun loadFromPrefs(): SelectedLocation {
        val sourceName = prefs.getString(KEY_SOURCE, null) ?: return SelectedLocation.None
        val source = runCatching { LocationSource.valueOf(sourceName) }.getOrNull()
            ?: return SelectedLocation.None
        if (!prefs.contains(KEY_LAT_BITS) || !prefs.contains(KEY_LNG_BITS)) {
            return SelectedLocation.None
        }
        val latitude = Double.fromBits(prefs.getLong(KEY_LAT_BITS, 0L))
        val longitude = Double.fromBits(prefs.getLong(KEY_LNG_BITS, 0L))
        return SelectedLocation(
            displayName = prefs.getString(KEY_DISPLAY_NAME, null),
            latitude = latitude,
            longitude = longitude,
            source = source,
        )
    }

    companion object {
        private const val PREFS_NAME = "aaspas_selected_location"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_LAT_BITS = "latitude_bits"
        private const val KEY_LNG_BITS = "longitude_bits"
        private const val KEY_SOURCE = "source"
        private const val KEY_SAVED_AT_MS = "saved_at_ms"
    }
}
