package com.aaspas.customer.core.network

import com.aaspas.customer.core.auth.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class UnauthorizedInterceptor(
    private val sessionManager: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            val path = chain.request().url.encodedPath
            val isAuthAttempt = path.endsWith("/auth/login") || path.endsWith("/auth/register")
            if (!isAuthAttempt) {
                sessionManager.clearSession()
            }
        }
        return response
    }
}
