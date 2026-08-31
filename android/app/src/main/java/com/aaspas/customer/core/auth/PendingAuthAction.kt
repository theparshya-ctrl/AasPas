package com.aaspas.customer.core.auth

/**
 * Holds a one-shot callback to run after the user completes login/register.
 * Avoids passing credentials or tokens through navigation arguments.
 */
object PendingAuthAction {
    var onAuthenticated: (() -> Unit)? = null

    fun set(action: () -> Unit) {
        onAuthenticated = action
    }

    fun runAndClear() {
        onAuthenticated?.invoke()
        onAuthenticated = null
    }

    fun clear() {
        onAuthenticated = null
    }
}
