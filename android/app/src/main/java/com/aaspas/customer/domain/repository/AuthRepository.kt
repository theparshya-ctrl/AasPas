package com.aaspas.customer.domain.repository

import com.aaspas.customer.core.auth.SessionState
import com.aaspas.customer.core.auth.UserRoles
import com.aaspas.customer.core.common.Result
import com.aaspas.customer.domain.model.UserAccount
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val sessionState: StateFlow<SessionState>

    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(
        email: String,
        password: String,
        fullName: String?,
        role: String = UserRoles.CUSTOMER,
    ): Result<Unit>
    suspend fun getCurrentUser(): Result<UserAccount>
    fun isLoggedIn(): Boolean
    fun logout()
}
