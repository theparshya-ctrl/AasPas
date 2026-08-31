package com.aaspas.customer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthTokenDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Long = 3600,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    @SerialName("full_name") val fullName: String? = null,
    val role: String = "customer",
)

@Serializable
data class UserAccountDto(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String? = null,
    val role: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String,
)
