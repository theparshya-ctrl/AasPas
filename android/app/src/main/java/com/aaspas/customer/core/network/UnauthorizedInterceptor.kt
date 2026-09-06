package com.aaspas.customer.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Legacy interceptor kept for response observation. Session clearing on 401 is handled by
 * [TokenRefreshAuthenticator] after a refresh attempt fails.
 */
class UnauthorizedInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request())
    }
}
