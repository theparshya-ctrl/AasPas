package com.aaspas.customer.presentation.common

import com.aaspas.customer.R
import com.aaspas.customer.core.common.AppError
import org.junit.Assert.assertEquals
import org.junit.Test

class AppErrorUiTest {

    @Test
    fun unexpectedUsesFallbackWhenProvided() {
        val error = AppError.Unexpected("JSON parse failed")
        assertEquals(R.string.error_shop_details, error.messageResId(R.string.error_shop_details))
    }

    @Test
    fun unexpectedDefaultsToGenericFallback() {
        val error = AppError.Unexpected("JSON parse failed")
        assertEquals(R.string.error_generic, error.messageResId())
    }

    @Test
    fun serverErrorKeepsServerMessageRes() {
        assertEquals(R.string.error_server, AppError.Server.messageResId(R.string.error_shop_details))
    }
}
