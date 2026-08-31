package com.aaspas.customer.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

private val errorJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class ApiErrorEnvelopeDto(
    val success: Boolean = false,
    val error: ApiErrorDetailDto? = null,
)

@Serializable
private data class ApiErrorDetailDto(
    val code: String = "",
    val message: String = "",
)

fun HttpException.readApiErrorMessage(fallback: String): String {
    val raw = response()?.errorBody()?.string().orEmpty()
    if (raw.isBlank()) return fallback
    return runCatching {
        errorJson.decodeFromString<ApiErrorEnvelopeDto>(raw).error?.message?.takeIf { it.isNotBlank() }
    }.getOrNull() ?: fallback
}
