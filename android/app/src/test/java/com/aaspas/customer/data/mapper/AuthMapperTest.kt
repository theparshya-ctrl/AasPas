package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.UserAccountDto
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthMapperTest {

    @Test
    fun `maps user dto to domain`() {
        val dto = UserAccountDto(
            id = "user-1",
            email = "alice@example.com",
            fullName = "Alice",
            role = "customer",
            createdAt = "2025-08-19T10:00:00Z",
        )

        val user = AuthMapper.toDomain(dto)

        assertEquals("alice@example.com", user.email)
        assertEquals("Alice", user.fullName)
        assertEquals("customer", user.role)
    }
}
