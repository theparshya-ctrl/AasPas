package com.aaspas.customer.core.common

import java.io.IOException
import java.net.SocketTimeoutException

sealed class AppError(val message: String) {
    data object NoInternet : AppError("Please check your internet connection.")
    data object Timeout : AppError("The request timed out. Please try again.")
    data object Server : AppError("Something went wrong. Please try again.")
    data object Unauthorized : AppError("Session expired. Please sign in again.")
    data object NotFound : AppError("This content is no longer available.")
    data class Unexpected(val detail: String) : AppError("Unable to load content.")
}

fun AppError.userMessage(): String = when (this) {
    is AppError.Unexpected -> detail
    else -> message
}

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val error: AppError) : Result<Nothing>()
}

fun Throwable.toAppError(): AppError = when (this) {
    is SocketTimeoutException -> AppError.Timeout
    is IOException -> AppError.NoInternet
    else -> AppError.Unexpected(localizedMessage ?: "Unknown error")
}
