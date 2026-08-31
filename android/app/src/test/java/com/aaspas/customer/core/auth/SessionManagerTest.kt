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
        sessionManager.saveSession("token-123", 3600)
        assertTrue(sessionManager.isLoggedIn())
        assertEquals("token-123", sessionManager.getToken())
        assertEquals(SessionState.LoggedIn, sessionManager.sessionState.value)
    }

    @Test
    fun `clear session removes token`() {
        sessionManager.saveSession("token-123", 3600)
        sessionManager.clearSession()
        assertFalse(sessionManager.isLoggedIn())
        assertNull(sessionManager.getToken())
        assertEquals(SessionState.LoggedOut, sessionManager.sessionState.value)
    }

    @Test
    fun `expired session is cleared`() {
        sessionManager.saveSession("token-123", -1)
        assertFalse(sessionManager.isLoggedIn())
        assertNull(sessionManager.getToken())
    }
}
