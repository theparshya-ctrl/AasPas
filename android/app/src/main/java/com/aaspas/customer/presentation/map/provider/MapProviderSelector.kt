package com.aaspas.customer.presentation.map.provider

/**
 * Chooses the active map tile provider.
 *
 * Google Maps is primary when [googleMapsKeyConfigured] is true and init has not failed.
 * OpenStreetMap is used when the key is missing or Google Maps cannot initialize.
 */
object MapProviderSelector {
    fun resolve(
        googleMapsKeyConfigured: Boolean,
        googleMapsInitFailed: Boolean,
    ): MapProviderType {
        return if (googleMapsKeyConfigured && !googleMapsInitFailed) {
            MapProviderType.Google
        } else {
            MapProviderType.OpenStreetMap
        }
    }
}
