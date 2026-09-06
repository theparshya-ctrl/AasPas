package com.aaspas.customer.data.repository

import android.content.Context
import com.aaspas.customer.core.auth.SessionManager
import com.aaspas.customer.core.auth.UserRoles
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.data.remote.AuthApi
import com.aaspas.customer.data.remote.dto.ApiResponseDto
import com.aaspas.customer.data.remote.dto.AuthTokenDto
import com.aaspas.customer.data.remote.dto.LoginRequestDto
import com.aaspas.customer.data.remote.dto.RefreshRequestDto
import com.aaspas.customer.data.remote.dto.RegisterRequestDto
import com.aaspas.customer.data.remote.dto.UserAccountDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AuthRepositoryImplTest {

    private lateinit var sessionManager: SessionManager
    private lateinit var api: CapturingAuthApi
    private lateinit var refreshApi: CapturingAuthApi
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("aaspas_session", Context.MODE_PRIVATE).edit().clear().apply()
        sessionManager = SessionManager(context)
        api = CapturingAuthApi()
        refreshApi = CapturingAuthApi()
        repository = AuthRepositoryImpl(api, refreshApi, sessionManager)
    }

    @Test
    fun `register defaults to customer role`() = runTest {
        val result = repository.register("alice@example.com", "password123", "Alice")
        assertTrue(result is Result.Success)
        assertEquals(UserRoles.CUSTOMER, api.lastRegister?.role)
    }

    @Test
    fun `register with explicit shop owner role`() = runTest {
        val result = repository.register(
            email = "owner@example.com",
            password = "password123",
            fullName = "Owner",
            role = UserRoles.SHOP_OWNER,
        )
        assertTrue(result is Result.Success)
        assertEquals(UserRoles.SHOP_OWNER, api.lastRegister?.role)
    }

    @Test
    fun `login persists refresh token`() = runTest {
        val result = repository.login("alice@example.com", "password123")
        assertTrue(result is Result.Success)
        assertTrue(sessionManager.isLoggedIn())
        assertEquals("refresh-123", sessionManager.getRefreshToken())
    }

    @Test
    fun `refresh session updates access token`() = runTest {
        sessionManager.saveSession("old-access", -1, "refresh-123", 3600)
        val result = repository.refreshSession()
        assertTrue(result is Result.Success)
        assertEquals("new-access", sessionManager.getToken())
    }

    private class CapturingAuthApi : AuthApi {
        var lastRegister: RegisterRequestDto? = null

        override suspend fun register(body: RegisterRequestDto): ApiResponseDto<UserAccountDto> {
            lastRegister = body
            return ApiResponseDto(
                success = true,
                data = UserAccountDto(
                    id = "user-1",
                    email = body.email,
                    fullName = body.fullName,
                    role = body.role,
                    createdAt = "2026-08-21T00:00:00Z",
                ),
            )
        }

        override suspend fun login(body: LoginRequestDto): ApiResponseDto<AuthTokenDto> {
            return ApiResponseDto(
                success = true,
                data = AuthTokenDto(
                    accessToken = "token-123",
                    expiresIn = 3600,
                    refreshToken = "refresh-123",
                    refreshExpiresIn = 2_592_000,
                ),
            )
        }

        override suspend fun refresh(body: RefreshRequestDto): ApiResponseDto<AuthTokenDto> {
            return ApiResponseDto(
                success = true,
                data = AuthTokenDto(
                    accessToken = "new-access",
                    expiresIn = 3600,
                    refreshToken = body.refreshToken,
                    refreshExpiresIn = 3600,
                ),
            )
        }

        override suspend fun getCurrentUser(): ApiResponseDto<UserAccountDto> {
            return ApiResponseDto(success = false, data = null)
        }
    }
}
