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

    /** Returns a valid access token, or null when access expired but refresh may still be valid. */
    fun getToken(): String? {
        if (isAccessTokenExpired()) return null
        return prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }
    }

    fun getRefreshToken(): String? {
        if (isRefreshTokenExpired()) return null
        return prefs.getString(KEY_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() }
    }

    fun isLoggedIn(): Boolean = hasValidAccessToken() || hasValidRefreshToken()

    fun saveSession(
        accessToken: String,
        expiresInSeconds: Long,
        refreshToken: String,
        refreshExpiresInSeconds: Long,
    ) {
        val accessExpiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        val refreshExpiresAt = System.currentTimeMillis() + (refreshExpiresInSeconds * 1000L)
        prefs.edit()
            .putString(KEY_TOKEN, accessToken)
            .putLong(KEY_EXPIRES_AT, accessExpiresAt)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_REFRESH_EXPIRES_AT, refreshExpiresAt)
            .apply()
        _sessionState.value = SessionState.LoggedIn
    }

    fun updateAccessToken(accessToken: String, expiresInSeconds: Long) {
        val accessExpiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        prefs.edit()
            .putString(KEY_TOKEN, accessToken)
            .putLong(KEY_EXPIRES_AT, accessExpiresAt)
            .apply()
        if (isLoggedIn()) {
            _sessionState.value = SessionState.LoggedIn
        }
    }

    /** @deprecated Use [saveSession] with refresh token metadata. */
    fun saveToken(token: String) {
        saveSession(token, DEFAULT_EXPIRES_IN_SECONDS, token, DEFAULT_REFRESH_EXPIRES_IN_SECONDS)
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_REFRESH_EXPIRES_AT)
            .apply()
        _sessionState.value = SessionState.LoggedOut
    }

    fun hasValidAccessToken(): Boolean {
        return !prefs.getString(KEY_TOKEN, null).isNullOrBlank() && !isAccessTokenExpired()
    }

    fun hasValidRefreshToken(): Boolean {
        return !prefs.getString(KEY_REFRESH_TOKEN, null).isNullOrBlank() && !isRefreshTokenExpired()
    }

    fun needsAccessTokenRefresh(): Boolean = hasValidRefreshToken() && isAccessTokenExpired()

    fun isAccessTokenExpired(): Boolean = isExpired(KEY_EXPIRES_AT)

    fun isRefreshTokenExpired(): Boolean = isExpired(KEY_REFRESH_EXPIRES_AT)

    private fun isExpired(expiresAtKey: String): Boolean {
        val expiresAt = prefs.getLong(expiresAtKey, 0L)
        if (expiresAt <= 0L) return false
        return System.currentTimeMillis() >= expiresAt
    }

    private fun resolveSessionState(): SessionState {
        return if (isLoggedIn()) SessionState.LoggedIn else SessionState.LoggedOut
    }

    companion object {
        private const val PREFS_NAME = "aaspas_session"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_EXPIRES_AT = "token_expires_at"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_REFRESH_EXPIRES_AT = "refresh_token_expires_at"
        private const val DEFAULT_EXPIRES_IN_SECONDS = 3600L
        private const val DEFAULT_REFRESH_EXPIRES_IN_SECONDS = 30L * 24L * 60L * 60L
    }
}
