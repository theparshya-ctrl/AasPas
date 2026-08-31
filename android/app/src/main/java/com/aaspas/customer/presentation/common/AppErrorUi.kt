package com.aaspas.customer.presentation.common

import com.aaspas.customer.R
import com.aaspas.customer.core.common.AppError

fun AppError.messageResId(fallbackResId: Int = R.string.error_generic): Int = when (this) {
    AppError.NoInternet -> R.string.error_network
    AppError.Timeout -> R.string.error_timeout
    AppError.Server -> R.string.error_server
    AppError.Unauthorized -> R.string.error_unauthorized
    AppError.NotFound -> R.string.error_not_found
    is AppError.Unexpected -> fallbackResId
}
