package com.aaspas.customer.core.update

import com.aaspas.customer.data.remote.dto.AppVersionDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateEvaluatorTest {

    @Test
    fun `same version returns null`() {
        val result = AppUpdateEvaluator.evaluate(
            currentVersionCode = 5,
            remote = AppVersionDto(
                latestVersionName = "0.1.2",
                latestVersionCode = 5,
                downloadUrl = "https://cdn.example/latest.apk",
            ),
        )
        assertNull(result)
    }

    @Test
    fun `newer version returns offer`() {
        val result = AppUpdateEvaluator.evaluate(
            currentVersionCode = 5,
            remote = AppVersionDto(
                latestVersionName = "0.1.3",
                latestVersionCode = 6,
                downloadUrl = "https://cdn.example/latest.apk",
                releaseNotes = listOf("Fix A", "Fix B"),
            ),
        )
        assertNotNull(result)
        assertEquals("0.1.3", result?.latestVersionName)
        assertEquals(6, result?.latestVersionCode)
        assertTrue(result?.releaseNotes?.size == 2)
    }

    @Test
    fun `invalid download url returns null`() {
        val result = AppUpdateEvaluator.evaluate(
            currentVersionCode = 1,
            remote = AppVersionDto(
                latestVersionName = "0.1.3",
                latestVersionCode = 6,
                downloadUrl = "http://insecure.example/latest.apk",
            ),
        )
        assertNull(result)
    }

    @Test
    fun `malformed remote payload returns null`() {
        assertNull(AppUpdateEvaluator.evaluate(5, null))
        assertNull(
            AppUpdateEvaluator.evaluate(
                5,
                AppVersionDto(latestVersionName = "", latestVersionCode = 0),
            ),
        )
    }
}
