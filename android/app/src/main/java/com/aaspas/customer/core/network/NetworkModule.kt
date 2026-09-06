package com.aaspas.customer.core.network

import android.content.Context
import com.aaspas.customer.BuildConfig
import com.aaspas.customer.core.auth.SessionManager
import com.aaspas.customer.data.remote.AuthApi
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

object NetworkModule {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val contentType = "application/json".toMediaType()

    @Volatile
    private var sessionManager: SessionManager? = null

    fun init(context: Context) {
        sessionManager = SessionManager(context.applicationContext)
    }

    fun session(): SessionManager {
        return sessionManager ?: error("NetworkModule.init(context) must be called first")
    }

    private fun baseClientBuilder(): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                            redactHeader("Authorization")
                        },
                    )
                }
            }
    }

    private val refreshOkHttpClient: OkHttpClient by lazy {
        baseClientBuilder().build()
    }

    private val refreshRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(refreshOkHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    private val refreshAuthApi: AuthApi by lazy { refreshRetrofit.create() }

    private val okHttpClient: OkHttpClient by lazy {
        val manager = session()
        baseClientBuilder()
            .authenticator(
                TokenRefreshAuthenticator(manager) {
                    TokenRefresher.refresh(manager, refreshAuthApi)
                },
            )
            .addInterceptor(AuthInterceptor(manager))
            .addInterceptor(UnauthorizedInterceptor())
            .build()
    }

    /** Customer discovery/details endpoints must not send auth — role-based owner JSON breaks parsing. */
    private val publicOkHttpClient: OkHttpClient by lazy { baseClientBuilder().build() }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    private val publicRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(publicOkHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    inline fun <reified T> api(): T = retrofit.create()

    fun refreshApi(): AuthApi = refreshAuthApi

    fun <T> publicApi(service: Class<T>): T = publicRetrofit.create(service)

    inline fun <reified T> publicApi(): T = publicApi(T::class.java)
}
