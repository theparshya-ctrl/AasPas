package com.aaspas.customer.core.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow(resolveSessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun getToken(): String? {
        if (isExpired()) {
            clearSession()
            return null
        }
        return prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }
    }

    fun isLoggedIn(): Boolean = getToken() != null

    fun saveSession(token: String, expiresInSeconds: Long) {
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
        _sessionState.value = SessionState.LoggedIn
    }

    /** @deprecated Use [saveSession] to persist expiry metadata. */
    fun saveToken(token: String) {
        saveSession(token, DEFAULT_EXPIRES_IN_SECONDS)
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
        _sessionState.value = SessionState.LoggedOut
    }

    fun isExpired(): Boolean {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (expiresAt <= 0L) return false
        return System.currentTimeMillis() >= expiresAt
    }

    private fun resolveSessionState(): SessionState {
        return if (prefs.getString(KEY_TOKEN, null).isNullOrBlank() || isExpired()) {
            SessionState.LoggedOut
        } else {
            SessionState.LoggedIn
        }
    }

    companion object {
        private const val PREFS_NAME = "aaspas_session"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_EXPIRES_AT = "token_expires_at"
        private const val DEFAULT_EXPIRES_IN_SECONDS = 3600L
    }
}
