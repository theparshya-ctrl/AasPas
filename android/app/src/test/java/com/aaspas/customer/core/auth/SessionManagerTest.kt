package com.aaspas.customer.core.auth

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionManagerTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("aaspas_session", Context.MODE_PRIVATE).edit().clear().apply()
        sessionManager = SessionManager(context)
    }

    @After
    fun tearDown() {
        sessionManager.clearSession()
    }

    @Test
    fun `starts logged out`() {
        assertFalse(sessionManager.isLoggedIn())
        assertEquals(SessionState.LoggedOut, sessionManager.sessionState.value)
    }

    @Test
    fun `save and restore session`() {
        sessionManager.saveSession("token-123", 3600, "refresh-123", 2_592_000)
        assertTrue(sessionManager.isLoggedIn())
        assertEquals("token-123", sessionManager.getToken())
        assertEquals("refresh-123", sessionManager.getRefreshToken())
        assertEquals(SessionState.LoggedIn, sessionManager.sessionState.value)
    }

    @Test
    fun `clear session removes tokens`() {
        sessionManager.saveSession("token-123", 3600, "refresh-123", 2_592_000)
        sessionManager.clearSession()
        assertFalse(sessionManager.isLoggedIn())
        assertNull(sessionManager.getToken())
        assertNull(sessionManager.getRefreshToken())
        assertEquals(SessionState.LoggedOut, sessionManager.sessionState.value)
    }

    @Test
    fun `expired access with valid refresh remains logged in`() {
        sessionManager.saveSession("token-123", -1, "refresh-123", 3600)
        assertTrue(sessionManager.isLoggedIn())
        assertNull(sessionManager.getToken())
        assertEquals("refresh-123", sessionManager.getRefreshToken())
        assertTrue(sessionManager.needsAccessTokenRefresh())
    }

    @Test
    fun `expired refresh clears logged-in state`() {
        sessionManager.saveSession("token-123", -1, "refresh-123", -1)
        assertFalse(sessionManager.isLoggedIn())
        assertNull(sessionManager.getToken())
        assertNull(sessionManager.getRefreshToken())
    }
}
