package com.aaspas.customer.testsupport

import com.aaspas.customer.core.auth.SessionState
import com.aaspas.customer.core.common.AppError
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.UserAccount
import com.aaspas.customer.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeAuthRepository(
    loggedIn: Boolean = false,
) : AuthRepository {
    private val _sessionState = MutableStateFlow(
        if (loggedIn) SessionState.LoggedIn else SessionState.LoggedOut,
    )
    override val sessionState: StateFlow<SessionState> = _sessionState

    var loginResult: Result<Unit> = Result.Success(Unit)
    var registerResult: Result<Unit> = Result.Success(Unit)
    var refreshResult: Result<Unit> = Result.Success(Unit)
    var restoreResult: Result<Unit> = Result.Success(Unit)
    var lastRegisterRole: String? = null
    var currentUserResult: Result<UserAccount> = Result.Failure(AppError.Unauthorized)
    var logoutCalled = false

    override suspend fun login(email: String, password: String): Result<Unit> {
        if (loginResult is Result.Success) {
            _sessionState.value = SessionState.LoggedIn
        }
        return loginResult
    }

    override suspend fun register(
        email: String,
        password: String,
        fullName: String?,
        role: String,
    ): Result<Unit> {
        lastRegisterRole = role
        if (registerResult is Result.Success) {
            _sessionState.value = SessionState.LoggedIn
        }
        return registerResult
    }

    override suspend fun getCurrentUser(): Result<UserAccount> = currentUserResult

    override suspend fun restoreSessionIfNeeded(): Result<Unit> = restoreResult

    override suspend fun refreshSession(): Result<Unit> = refreshResult

    override fun isLoggedIn(): Boolean = _sessionState.value == SessionState.LoggedIn

    override fun logout() {
        logoutCalled = true
        _sessionState.value = SessionState.LoggedOut
    }
}
