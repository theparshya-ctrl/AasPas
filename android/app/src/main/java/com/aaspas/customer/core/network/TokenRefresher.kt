package com.aaspas.customer.core.network

import com.aaspas.customer.core.auth.SessionManager
import com.aaspas.customer.data.remote.AuthApi
import com.aaspas.customer.data.remote.dto.RefreshRequestDto
import kotlinx.coroutines.runBlocking

internal object TokenRefresher {
    fun refresh(sessionManager: SessionManager, refreshApi: AuthApi): Boolean {
        val refreshToken = sessionManager.getRefreshToken() ?: return false
        return runBlocking {
            runCatching {
                val response = refreshApi.refresh(RefreshRequestDto(refreshToken))
                val data = response.data
                if (!response.success || data == null || data.accessToken.isBlank()) {
                    return@runBlocking false
                }
                sessionManager.updateAccessToken(data.accessToken, data.expiresIn)
                if (!data.refreshToken.isNullOrBlank() && data.refreshExpiresIn > 0) {
                    sessionManager.saveSession(
                        accessToken = data.accessToken,
                        expiresInSeconds = data.expiresIn,
                        refreshToken = data.refreshToken,
                        refreshExpiresInSeconds = data.refreshExpiresIn,
                    )
                }
                true
            }.getOrDefault(false)
        }
    }
}
