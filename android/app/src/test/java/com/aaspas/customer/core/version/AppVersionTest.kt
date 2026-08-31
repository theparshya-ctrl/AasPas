package com.aaspas.customer.core.version

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun `debug metadata maps to DEV identity and compact indicator`() {
        val identity = AppVersion.from(
            environment = "DEV",
            versionName = "0.1.0",
            versionCode = 1,
        )

        assertEquals("AasPas", identity.productName)
        assertEquals("AasPas DEV", identity.brandedName)
        assertEquals(AppVersion.ENVIRONMENT_DEV, identity.environment)
        assertEquals("0.1.0", identity.versionName)
        assertEquals("0.1.0", identity.versionLine)
        assertEquals(1, identity.versionCode)
        assertEquals("1", identity.buildLine)
        assertTrue(identity.isDev)
        assertEquals("DEV • v0.1.0 (1)", identity.compactIndicator)
    }

    @Test
    fun `dev environment label is case insensitive`() {
        val identity = AppVersion.from(
            environment = "dev",
            versionName = "0.1.0",
            versionCode = 1,
        )
        assertTrue(identity.isDev)
        assertEquals("DEV", identity.environment)
    }

    @Test
    fun `release metadata has no DEV branding`() {
        val identity = AppVersion.from(
            environment = "PRODUCTION",
            versionName = "0.1.0",
            versionCode = 1,
        )

        assertEquals("AasPas", identity.brandedName)
        assertEquals(AppVersion.ENVIRONMENT_PRODUCTION, identity.environment)
        assertFalse(identity.isDev)
        assertNull(identity.compactIndicator)
        assertEquals("0.1.0", identity.versionLine)
        assertEquals("1", identity.buildLine)
    }

    @Test
    fun `unknown environment is treated as production`() {
        val identity = AppVersion.from(
            environment = "staging",
            versionName = "1.2.3",
            versionCode = 12,
        )
        assertFalse(identity.isDev)
        assertEquals(AppVersion.ENVIRONMENT_PRODUCTION, identity.environment)
        assertNull(identity.compactIndicator)
        assertEquals("AasPas", identity.brandedName)
    }

    @Test
    fun `debugBuild flag shows DEV identity`() {
        val identity = AppVersion.from(
            environment = "PRODUCTION",
            versionName = "0.1.0",
            versionCode = 1,
            debugBuild = true,
        )
        assertTrue(identity.isDev)
        assertEquals("DEV", identity.environment)
        assertEquals("DEV • v0.1.0 (1)", identity.compactIndicator)
    }
}
