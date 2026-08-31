package com.aaspas.customer.domain.model

data class UserAccount(
    val id: String,
    val email: String,
    val fullName: String?,
    val role: String,
    val createdAt: String,
)
