package com.aaspas.customer.core.network

import com.aaspas.customer.BuildConfig

/**
 * Rewrites locally uploaded `/media/...` URLs to use the app's configured API base URL.
 * Fixes customer image loading when stored photo URLs use a different host (Wi-Fi vs Tailscale).
 */
object MediaUrlResolver {
    private const val MEDIA_MARKER = "/media/"

    fun resolve(photoUrl: String?): String? {
        val trimmed = photoUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (trimmed.startsWith(MEDIA_MARKER)) {
            return "${BuildConfig.API_BASE_URL.trimEnd('/')}$trimmed"
        }
        val markerIndex = trimmed.indexOf(MEDIA_MARKER)
        if (markerIndex < 0) {
            return trimmed
        }
        val mediaPath = trimmed.substring(markerIndex + MEDIA_MARKER.length).trimStart('/')
        if (mediaPath.isEmpty()) {
            return trimmed
        }
        return "${BuildConfig.API_BASE_URL.trimEnd('/')}$MEDIA_MARKER$mediaPath"
    }
}
