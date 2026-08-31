package com.aaspas.customer.core.navigation

import android.content.Intent
import android.net.Uri

object DirectionsIntentBuilder {
    fun build(latitude: Double?, longitude: Double?, label: String? = null): Intent? {
        if (latitude == null || longitude == null) return null
        val encodedLabel = label?.trim()?.takeIf { it.isNotEmpty() }?.let { Uri.encode(it) }
        val query = if (encodedLabel != null) {
            "$latitude,$longitude($encodedLabel)"
        } else {
            "$latitude,$longitude"
        }
        return Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query")).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildFallback(latitude: Double?, longitude: Double?, label: String? = null): Intent? {
        if (latitude == null || longitude == null) return null
        val encodedLabel = label?.trim()?.takeIf { it.isNotEmpty() }?.let { Uri.encode(it) }
        val query = if (encodedLabel != null) {
            "$latitude,$longitude($encodedLabel)"
        } else {
            "$latitude,$longitude"
        }
        return Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
