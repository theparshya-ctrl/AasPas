package com.aaspas.customer.data.mapper

import com.aaspas.customer.data.remote.dto.UserAccountDto
import com.aaspas.customer.domain.model.UserAccount

object AuthMapper {
    fun toDomain(dto: UserAccountDto): UserAccount = UserAccount(
        id = dto.id,
        email = dto.email,
        fullName = dto.fullName,
        role = dto.role,
        createdAt = dto.createdAt,
    )
}
