package com.aaspas.customer.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaUrlResolverTest {

    @Test
    fun `returns null for blank url`() {
        assertNull(MediaUrlResolver.resolve(null))
        assertNull(MediaUrlResolver.resolve("  "))
    }

    @Test
    fun `rewrites media path to configured api base host`() {
        val resolved = MediaUrlResolver.resolve("http://192.168.1.99:8000/media/shops/abc/photo.jpg")
        assertEquals(
            "${com.aaspas.customer.BuildConfig.API_BASE_URL.trimEnd('/')}/media/shops/abc/photo.jpg",
            resolved,
        )
    }

    @Test
    fun `rewrites relative media path to configured api base host`() {
        val resolved = MediaUrlResolver.resolve("/media/shops/abc/photo.jpg")
        assertEquals(
            "${com.aaspas.customer.BuildConfig.API_BASE_URL.trimEnd('/')}/media/shops/abc/photo.jpg",
            resolved,
        )
    }

    @Test
    fun `leaves external urls unchanged`() {
        val external = "https://cdn.example.demo/shops/demo.jpg"
        assertEquals(external, MediaUrlResolver.resolve(external))
    }
}
