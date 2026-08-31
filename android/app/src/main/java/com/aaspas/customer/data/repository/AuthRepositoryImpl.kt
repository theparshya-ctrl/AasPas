package com.aaspas.customer.data.repository

import com.aaspas.customer.core.auth.SessionManager
import com.aaspas.customer.core.auth.SessionState
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.core.common.toAppError
import com.aaspas.customer.data.mapper.AuthMapper
import com.aaspas.customer.data.remote.AuthApi
import com.aaspas.customer.data.remote.dto.LoginRequestDto
import com.aaspas.customer.data.remote.dto.RegisterRequestDto
import com.aaspas.customer.domain.model.UserAccount
import com.aaspas.customer.domain.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val sessionManager: SessionManager,
) : AuthRepository {
    override val sessionState: StateFlow<SessionState> = sessionManager.sessionState

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = api.login(LoginRequestDto(email.trim(), password))
            val tokenData = response.data
            if (!response.success || tokenData?.accessToken.isNullOrBlank()) {
                Result.Failure(AppError.Server)
            } else {
                sessionManager.saveSession(tokenData.accessToken, tokenData.expiresIn)
                Result.Success(Unit)
            }
        } catch (http: HttpException) {
            Result.Failure(mapHttpError(http))
        } catch (throwable: Throwable) {
            Result.Failure(throwable.toAppError())
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        fullName: String?,
        role: String,
    ): Result<Unit> {
        return try {
            val response = api.register(
                RegisterRequestDto(
                    email = email.trim(),
                    password = password,
                    fullName = fullName?.trim()?.takeIf { it.isNotBlank() },
                    role = role,
                ),
            )
            if (!response.success) {
                Result.Failure(AppError.Server)
            } else {
                login(email, password)
            }
        } catch (http: HttpException) {
            Result.Failure(mapHttpError(http))
        } catch (throwable: Throwable) {
            Result.Failure(throwable.toAppError())
        }
    }

    override suspend fun getCurrentUser(): Result<UserAccount> {
        return try {
            val response = api.getCurrentUser()
            val user = response.data
            if (!response.success || user == null) {
                Result.Failure(AppError.Server)
            } else {
                Result.Success(AuthMapper.toDomain(user))
            }
        } catch (http: HttpException) {
            Result.Failure(mapHttpError(http))
        } catch (throwable: Throwable) {
            Result.Failure(throwable.toAppError())
        }
    }

    override fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    override fun logout() {
        sessionManager.clearSession()
    }

    private fun mapHttpError(http: HttpException): AppError {
        return when (http.code()) {
            401 -> AppError.Unauthorized
            409 -> AppError.Unexpected("Email already registered")
            422 -> AppError.Unexpected("Invalid registration details")
            else -> AppError.Unexpected("Request failed (${http.code()})")
        }
    }
}
